package com.barinventory.invoice.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts raw text from digital PDF files using Apache PDFBox. Works only for
 * digitally generated PDFs (text layer present). Government ICDC invoices are
 * always digital — not scanned.
 */
@Service
@Slf4j
public class PDFBoxExtractorService {

	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
	private static final String PDF_MAGIC = "%PDF";

	/**
	 * Extracts all text from PDF. setSortByPosition(true) is critical — ensures
	 * table rows come out in correct left-to-right, top-to-bottom order.
	 */
	public String extractText(MultipartFile file) throws IOException {
		log.info("Extracting text from: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

		try (InputStream is = file.getInputStream(); PDDocument document = PDDocument.load(is)) {

			if (document.isEncrypted()) {
				throw new IllegalArgumentException("PDF is password protected. Please upload an unencrypted PDF.");
			}

			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true); // critical for ICDC table row order
			stripper.setStartPage(1);
			stripper.setEndPage(document.getNumberOfPages());

			String text = stripper.getText(document);

			if (text == null || text.isBlank()) {
				throw new IllegalArgumentException(
						"No text found in PDF. Only digital PDFs are supported — not scanned images.");
			}

			log.info("Extraction complete. Pages: {}, Characters: {}", document.getNumberOfPages(), text.length());

			return text;
		}
	}

	/**
	 * Validates uploaded file is a real PDF. Checks: not null, not empty, size
	 * limit, .pdf extension, PDF magic bytes.
	 */
	public boolean isValidPDF(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			log.warn("Validation failed — file is null or empty");
			return false;
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			log.warn("Validation failed — file too large: {} bytes", file.getSize());
			return false;
		}

		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
			log.warn("Validation failed — not a PDF file: {}", filename);
			return false;
		}

		// Check PDF magic bytes (%PDF) at start of file
		try (InputStream is = file.getInputStream()) {
			byte[] header = new byte[4];
			if (is.read(header) < 4)
				return false;
			if (!new String(header).startsWith(PDF_MAGIC)) {
				log.warn("Validation failed — PDF magic bytes not found");
				return false;
			}
		} catch (IOException e) {
			log.error("Validation error reading file: {}", e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Returns page count — used for logging.
	 */
	public int getPageCount(MultipartFile file) throws IOException {
		try (InputStream is = file.getInputStream(); PDDocument document = PDDocument.load(is)) {
			return document.getNumberOfPages();
		}
	}
}