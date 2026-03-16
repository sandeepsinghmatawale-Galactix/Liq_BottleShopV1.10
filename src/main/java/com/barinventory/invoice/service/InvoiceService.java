package com.barinventory.invoice.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.barinventory.brands.service.BrandService;
import com.barinventory.invoice.dto.ExtractedInvoiceData;
import com.barinventory.invoice.dto.ExtractedItemData;
import com.barinventory.invoice.dto.ExtractedItemResponse;
import com.barinventory.invoice.dto.ExtractionResultResponse;
import com.barinventory.invoice.dto.InvoiceConfirmRequest;
import com.barinventory.invoice.dto.InvoiceItemConfirmRequest;
import com.barinventory.invoice.dto.InvoiceItemResponse;
import com.barinventory.invoice.dto.InvoiceResponse;
import com.barinventory.invoice.dto.InvoiceSummaryResponse;
import com.barinventory.invoice.entity.Invoice;
import com.barinventory.invoice.entity.InvoiceItem;
import com.barinventory.invoice.entity.InvoiceStatus;
import com.barinventory.invoice.port.StockroomPort;
import com.barinventory.invoice.repository.InvoiceItemRepository;
import com.barinventory.invoice.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {

	private final PDFBoxExtractorService pdfBoxExtractorService;
	private final InvoiceParserService invoiceParserService;
	private final BrandMatcherService brandMatcherService;
	private final InvoiceRepository invoiceRepository;
	private final InvoiceItemRepository invoiceItemRepository;
	private final InvoiceFileStorageService fileStorageService;

	// StockroomPort — interface keeps invoice module decoupled from stockroom
	// module
	private final StockroomPort stockroomPort;

	private final BrandService brandService;

	// ── 1. PDF Upload & Extract ───────────────────────────────────────────────

	/**
	 * Main upload flow: - Validates PDF - Stores file on disk - Extracts text via
	 * PDFBox - Parses into structured data using ICDC parser - Fuzzy matches brands
	 * against master list - Checks for duplicate invoice number - Saves as PENDING
	 * invoice (stockReceivedDate defaults to today) - Returns
	 * ExtractionResultResponse for owner review screen
	 */
	public ExtractionResultResponse uploadAndExtract(MultipartFile file, String uploadedBy,
			List<BrandMatcherService.BrandMasterRef> masterBrands) {

		log.info("Processing invoice PDF upload: {} by {}", file.getOriginalFilename(), uploadedBy);

		// Step 1 — Validate PDF (extension + magic bytes + size)
		if (!pdfBoxExtractorService.isValidPDF(file)) {
			throw new IllegalArgumentException("Invalid PDF file. Please upload a valid PDF under 10MB.");
		}

		// Step 2 — Store file on disk before extraction
		String storedFilePath = fileStorageService.storeFile(file);

		// Step 3 — Extract raw text via PDFBox
		String rawText;
		try {
			rawText = pdfBoxExtractorService.extractText(file);
		} catch (IOException e) {
			fileStorageService.deleteFile(storedFilePath); // cleanup on failure
			throw new RuntimeException("Failed to read PDF content: " + e.getMessage(), e);
		}

		// Step 4 — Parse extracted text using ICDC parser
		ExtractedInvoiceData extracted = invoiceParserService.parse(rawText);

		if (!extracted.isExtractionSuccess()) {
			fileStorageService.deleteFile(storedFilePath);
			throw new RuntimeException("PDF parsing failed: " + extracted.getExtractionMessage());
		}

		// Step 5 — Fuzzy match brands against master list
		if (masterBrands != null && !masterBrands.isEmpty()) {
			extracted.getItems().forEach(item -> brandMatcherService.matchBrand(item, masterBrands));
		}

		// Step 6 — Duplicate invoice check
		if (extracted.getInvoiceNumber() != null
				&& invoiceRepository.existsByInvoiceNumber(extracted.getInvoiceNumber())) {
			fileStorageService.deleteFile(storedFilePath);
			throw new IllegalStateException(
					"Invoice " + extracted.getInvoiceNumber() + " already exists in the system.");
		}

		// Step 7 — Save as PENDING
		// stockReceivedDate defaults to today — owner changes on review screen
		Invoice savedInvoice = saveAsPending(extracted, file.getOriginalFilename(), storedFilePath, uploadedBy);

		log.info("Invoice saved as PENDING. ID: {}, Invoice#: {}", savedInvoice.getId(),
				savedInvoice.getInvoiceNumber());

		// Step 8 — Build and return response for review screen
		return buildExtractionResponse(savedInvoice, extracted);
	}

	// ── 2. Confirm Invoice & Post to Stockroom ────────────────────────────────

	/**
	 * Called when owner has reviewed extracted data on the frontend review screen.
	 * Owner enters actual received quantities, breakage, and confirms
	 * stockReceivedDate.
	 *
	 * CRITICAL: Stock posted to stockroom using stockReceivedDate — NOT
	 * invoiceDate. This is the actual day the vehicle arrived and stock was
	 * physically unloaded.
	 */
	public InvoiceResponse confirmInvoice(InvoiceConfirmRequest request, String confirmedBy) {
		log.info("Confirming invoice ID: {} by {}", request.getInvoiceId(), confirmedBy);

		// Load invoice with all items — single query via JOIN FETCH
		Invoice invoice = invoiceRepository.findByIdWithItems(request.getInvoiceId())
				.orElseThrow(() -> new RuntimeException("Invoice not found: " + request.getInvoiceId()));

		// Guard — prevent double confirmation
		if (invoice.getStatus() == InvoiceStatus.CONFIRMED || invoice.getStatus() == InvoiceStatus.DISCREPANCY) {
			throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber() + " is already confirmed.");
		}

		// Validate stockReceivedDate — must be present, not future, not before
		// invoiceDate
		validateStockReceivedDate(request.getStockReceivedDate(), invoice.getInvoiceDate());

		// Update invoice header fields from owner's input
		invoice.setStockReceivedDate(request.getStockReceivedDate());
		invoice.setVehicleCharges(request.getVehicleCharges());
		invoice.setVehicleNumber(request.getVehicleNumber());
		invoice.setRemarks(request.getRemarks());
		invoice.setConfirmedBy(confirmedBy);
		invoice.setConfirmedAt(LocalDateTime.now());

		// Update each line item with actual received quantities from owner
		updateItemQuantities(invoice, request.getItems());

		// Set final status — DISCREPANCY if any shortage or breakage found
		InvoiceStatus finalStatus = invoice.hasDiscrepancy() ? InvoiceStatus.DISCREPANCY : InvoiceStatus.CONFIRMED;
		invoice.setStatus(finalStatus);

		Invoice saved = invoiceRepository.save(invoice);

		// Post confirmed stock to stockroom using stockReceivedDate
		postToStockroom(saved);

		log.info("Invoice {} confirmed. Status: {}. Stock posted for date: {}", saved.getInvoiceNumber(), finalStatus,
				saved.getStockReceivedDate());

		return mapToInvoiceResponse(saved);
	}

	// ── 3. Bulk Upload ────────────────────────────────────────────────────────

	/**
	 * Process multiple PDFs in one request. Each PDF extracted independently —
	 * failures don't stop other files. Returns one ExtractionResultResponse per
	 * file. Owner reviews and confirms each separately via /confirm endpoint.
	 */
	public List<ExtractionResultResponse> bulkUploadAndExtract(List<MultipartFile> files, String uploadedBy,
			List<BrandMatcherService.BrandMasterRef> masterBrands) {

		log.info("Bulk upload: {} files by {}", files.size(), uploadedBy);

		List<ExtractionResultResponse> results = new ArrayList<>();

		for (MultipartFile file : files) {
			try {
				ExtractionResultResponse result = uploadAndExtract(file, uploadedBy, masterBrands);
				results.add(result);
				log.info("Bulk: processed {} successfully", file.getOriginalFilename());
			} catch (Exception e) {
				log.error("Bulk: failed to process {}: {}", file.getOriginalFilename(), e.getMessage());
				// Add failed entry — frontend shows which file failed and why
				results.add(ExtractionResultResponse.builder().extractionSuccess(false)
						.extractionMessage("Failed: " + e.getMessage()).pdfFileName(file.getOriginalFilename())
						.build());
			}
		}

		long successCount = results.stream().filter(ExtractionResultResponse::isExtractionSuccess).count();
		log.info("Bulk upload complete. Success: {}/{}", successCount, files.size());

		return results;
	}

	// ── 4. Query Methods ──────────────────────────────────────────────────────

	@Transactional(readOnly = true)
	public List<InvoiceSummaryResponse> getAllInvoices() {
		return invoiceRepository.findAllByOrderByStockReceivedDateDesc().stream().map(this::mapToSummaryResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<InvoiceSummaryResponse> getPendingInvoices() {
		return invoiceRepository.findAllPendingInvoices().stream().map(this::mapToSummaryResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public InvoiceResponse getInvoiceById(Long id) {
		Invoice invoice = invoiceRepository.findByIdWithItems(id)
				.orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
		return mapToInvoiceResponse(invoice);
	}

	@Transactional(readOnly = true)
	public List<InvoiceSummaryResponse> getInvoicesByDateRange(LocalDate from, LocalDate to) {
		return invoiceRepository.findByStockReceivedDateBetweenOrderByStockReceivedDateDesc(from, to).stream()
				.map(this::mapToSummaryResponse).collect(Collectors.toList());
	}

	// ── 5. PDF Download ───────────────────────────────────────────────────────

	/**
	 * Returns raw PDF bytes for the given invoice. Used by controller's /pdf
	 * endpoint — frontend PDF.js renders it.
	 */
	@Transactional(readOnly = true)
	public byte[] getInvoicePdf(Long invoiceId) {
		Invoice invoice = invoiceRepository.findById(invoiceId)
				.orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

		if (invoice.getPdfFilePath() == null) {
			throw new RuntimeException("No PDF file stored for invoice: " + invoiceId);
		}

		try {
			return fileStorageService.loadFile(invoice.getPdfFilePath());
		} catch (IOException e) {
			throw new RuntimeException("Could not load invoice PDF: " + e.getMessage(), e);
		}
	}

	// ── Private Helpers ───────────────────────────────────────────────────────

	private Invoice saveAsPending(ExtractedInvoiceData extracted, String originalFileName, String storedFilePath,
			String uploadedBy) {

		Invoice invoice = Invoice.builder().invoiceNumber(extracted.getInvoiceNumber())
				.depotName(extracted.getDepotName()).retailerName(extracted.getRetailerName())
				.retailerCode(extracted.getRetailerCode()).licenseNumber(extracted.getLicenseNumber())
				.invoiceDate(extracted.getInvoiceDate()).stockReceivedDate(LocalDate.now()) // default today — owner
																							// changes in review
				.totalAmount(extracted.getTotalAmount()).summaryTotalCases(extracted.getSummaryTotalCases())
				.summaryBreakageCases(extracted.getSummaryBreakageCases())
				.summaryShortageCases(extracted.getSummaryShortageCases()).pdfFileName(originalFileName)
				.pdfFilePath(storedFilePath).status(InvoiceStatus.PENDING).uploadedBy(uploadedBy).build();

		// Map each extracted item to InvoiceItem entity
		if (extracted.getItems() != null) {
			extracted.getItems().forEach(extractedItem -> {
				InvoiceItem item = InvoiceItem.builder().brandNameRaw(extractedItem.getBrandNameRaw())
						.brandNameMatched(extractedItem.getBrandNameMatched())
						.brandMasterId(extractedItem.getBrandMasterId()).sizeMl(extractedItem.getSizeMl())
						.productType(extractedItem.getProductType()).packType(extractedItem.getPackType())
						.bottlesPerCase(
								extractedItem.getBottlesPerCase() != null ? extractedItem.getBottlesPerCase() : 12)
						.invoicedCases(extractedItem.getInvoicedCases())
						.invoicedBottles(extractedItem.getInvoicedBottles())
						.receivedCases(extractedItem.getInvoicedCases()) // default full qty
						.breakageQty(0).mrpPerBottle(extractedItem.getMrpPerBottle())
						.ratePerCase(extractedItem.getRatePerCase()).lineTotal(extractedItem.getLineTotal())
						.postedToStockroom(false).build();

				item.calculateDerivedFields();
				invoice.addItem(item);
			});
		}

		return invoiceRepository.save(invoice);
	}

	private void updateItemQuantities(Invoice invoice, List<InvoiceItemConfirmRequest> itemRequests) {
		if (itemRequests == null)
			return;

		itemRequests.forEach(req -> invoice.getItems().stream().filter(item -> item.getId().equals(req.getItemId()))
				.findFirst().ifPresent(item -> {
					item.setReceivedCases(req.getReceivedCases());
					item.setBreakageQty(req.getBreakageQty() != null ? req.getBreakageQty() : 0);

					// Override brand match if owner corrected it on review screen
					if (req.getBrandMasterId() != null) {
						item.setBrandMasterId(req.getBrandMasterId());
						item.setBrandNameMatched(req.getBrandNameMatched());
					}

					item.calculateDerivedFields();
				}));
	}

	private void postToStockroom(Invoice invoice) {
		invoice.getItems().forEach(item -> {
			// Only post items that have received bottles and not already posted
			if (Boolean.FALSE.equals(item.getPostedToStockroom()) && item.getReceivedBottles() != null
					&& item.getReceivedBottles() > 0) {
				try {
					stockroomPort.addStock(item.getBrandMasterId(),
							item.getBrandNameMatched() != null ? item.getBrandNameMatched() : item.getBrandNameRaw(),
							item.getSizeMl(), item.getReceivedBottles(), // net bottles after breakage
							invoice.getStockReceivedDate(), // STOCK RECEIVED DATE — not invoiceDate
							"INVOICE:" + invoice.getInvoiceNumber());
					item.setPostedToStockroom(true);
					log.info("Stockroom updated — Brand: {}, Bottles: {}, Date: {}", item.getBrandNameRaw(),
							item.getReceivedBottles(), invoice.getStockReceivedDate());
				} catch (Exception e) {
					log.error("Stockroom post failed for item {}: {}", item.getBrandNameRaw(), e.getMessage());
					throw new RuntimeException("Stockroom posting failed for: " + item.getBrandNameRaw(), e);
				}
			}
		});
	}

	private void validateStockReceivedDate(LocalDate stockReceivedDate, LocalDate invoiceDate) {
		if (stockReceivedDate == null) {
			throw new IllegalArgumentException("Stock Received Date is required.");
		}
		if (stockReceivedDate.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Stock Received Date cannot be a future date.");
		}
		if (invoiceDate != null && stockReceivedDate.isBefore(invoiceDate)) {
			throw new IllegalArgumentException("Stock Received Date (" + stockReceivedDate
					+ ") cannot be before Invoice Date (" + invoiceDate + ").");
		}
	}

	// ── Mappers ───────────────────────────────────────────────────────────────

	private ExtractionResultResponse buildExtractionResponse(Invoice invoice, ExtractedInvoiceData extracted) {

		List<ExtractedItemResponse> itemResponses = invoice.getItems().stream()
				.map(item -> ExtractedItemResponse.builder().itemId(item.getId()).brandNameRaw(item.getBrandNameRaw())
						.brandNameMatched(item.getBrandNameMatched()).brandMasterId(item.getBrandMasterId())
						.matchConfident(
								// Pull matchConfident flag from parsed data by brand name
								extracted.getItems().stream()
										.filter(e -> e.getBrandNameRaw() != null
												&& e.getBrandNameRaw().equals(item.getBrandNameRaw()))
										.map(ExtractedItemData::isMatchConfident).findFirst().orElse(false))
						.sizeMl(item.getSizeMl()).invoicedCases(item.getInvoicedCases())
						.bottlesPerCase(item.getBottlesPerCase()).invoicedBottles(item.getInvoicedBottles())
						.mrpPerBottle(item.getMrpPerBottle()).ratePerCase(item.getRatePerCase())
						.lineTotal(item.getLineTotal()).productType(item.getProductType()).packType(item.getPackType())
						.build())
				.collect(Collectors.toList());

		return ExtractionResultResponse.builder().invoiceId(invoice.getId()).invoiceNumber(invoice.getInvoiceNumber())
				.depotName(invoice.getDepotName()).invoiceDate(invoice.getInvoiceDate())
				.stockReceivedDate(invoice.getStockReceivedDate()).totalAmount(invoice.getTotalAmount())
				.pdfFileName(invoice.getPdfFileName()).retailerName(invoice.getRetailerName())
				.retailerCode(invoice.getRetailerCode()).licenseNumber(invoice.getLicenseNumber())
				.summaryTotalCases(extracted.getSummaryTotalCases())
				.summaryBreakageCases(extracted.getSummaryBreakageCases())
				.summaryShortageCases(extracted.getSummaryShortageCases()).totalsMismatch(extracted.isTotalsMismatch())
				.extractedItems(itemResponses).extractionSuccess(true).totalLinesExtracted(itemResponses.size())
				.build();
	}

	private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
		List<InvoiceItemResponse> itemResponses = invoice.getItems().stream()
				.map(item -> InvoiceItemResponse.builder().id(item.getId()).brandNameRaw(item.getBrandNameRaw())
						.brandNameMatched(item.getBrandNameMatched()).brandMasterId(item.getBrandMasterId())
						.sizeMl(item.getSizeMl()).productType(item.getProductType()).packType(item.getPackType())
						.invoicedCases(item.getInvoicedCases()).bottlesPerCase(item.getBottlesPerCase())
						.invoicedBottles(item.getInvoicedBottles()).receivedCases(item.getReceivedCases())
						.receivedBottles(item.getReceivedBottles()).breakageQty(item.getBreakageQty())
						.shortageCases(item.getShortageCases()).mrpPerBottle(item.getMrpPerBottle())
						.ratePerCase(item.getRatePerCase()).lineTotal(item.getLineTotal())
						.postedToStockroom(item.getPostedToStockroom()).hasShortage(item.hasShortage())
						.hasBreakage(item.hasBreakage()).build())
				.collect(Collectors.toList());

		int totalInvoiced = invoice.getItems().stream()
				.mapToInt(i -> i.getInvoicedCases() != null ? i.getInvoicedCases() : 0).sum();
		int totalReceived = invoice.getItems().stream()
				.mapToInt(i -> i.getReceivedCases() != null ? i.getReceivedCases() : 0).sum();
		int totalBreakage = invoice.getItems().stream()
				.mapToInt(i -> i.getBreakageQty() != null ? i.getBreakageQty() : 0).sum();

		return InvoiceResponse.builder().id(invoice.getId()).invoiceNumber(invoice.getInvoiceNumber())
				.depotName(invoice.getDepotName()).invoiceDate(invoice.getInvoiceDate())
				.stockReceivedDate(invoice.getStockReceivedDate()).uploadedAt(invoice.getUploadedAt())
				.totalAmount(invoice.getTotalAmount()).vehicleCharges(invoice.getVehicleCharges())
				.vehicleNumber(invoice.getVehicleNumber()).pdfFileName(invoice.getPdfFileName())
				.status(invoice.getStatus()).remarks(invoice.getRemarks()).uploadedBy(invoice.getUploadedBy())
				.confirmedBy(invoice.getConfirmedBy()).confirmedAt(invoice.getConfirmedAt()).items(itemResponses)
				.hasDiscrepancy(invoice.hasDiscrepancy()).totalInvoicedCases(totalInvoiced)
				.totalReceivedCases(totalReceived).totalBreakage(totalBreakage).build();
	}

	private InvoiceSummaryResponse mapToSummaryResponse(Invoice invoice) {
		int totalCases = invoice.getItems().stream()
				.mapToInt(i -> i.getInvoicedCases() != null ? i.getInvoicedCases() : 0).sum();

		return InvoiceSummaryResponse.builder().id(invoice.getId()).invoiceNumber(invoice.getInvoiceNumber())
				.depotName(invoice.getDepotName()).invoiceDate(invoice.getInvoiceDate())
				.stockReceivedDate(invoice.getStockReceivedDate()).status(invoice.getStatus())
				.totalItems(invoice.getItems().size()).totalCases(totalCases).hasDiscrepancy(invoice.hasDiscrepancy())
				.uploadedAt(invoice.getUploadedAt()).build();
	}

	private List<BrandMatcherService.BrandMasterRef> getMasterBrands() {
		return brandService.getAllActiveBrands().stream().filter(brand -> brand.getSizes() != null)
				.flatMap(
						brand -> brand.getSizes().stream().filter(size -> size.isActive() && size.getVolumeMl() != null)
								.map(size -> new BrandMatcherService.BrandMasterRef(size.getId(), // BrandSize.id → used
																									// as brandMasterId
										brand.getBrandName(), // Brand.brandName → matched against PDF
										size.getVolumeMl() // BrandSize.volumeMl → sizeMl bridge
								))).toList();
	}
}
