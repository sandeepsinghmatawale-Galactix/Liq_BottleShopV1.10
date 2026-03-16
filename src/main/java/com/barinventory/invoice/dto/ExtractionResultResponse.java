package com.barinventory.invoice.dto;

 

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO — returned after PDF upload and AI extraction. Frontend uses
 * this to populate the review screen. Owner reviews, corrects if needed, then
 * calls /confirm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionResultResponse {

	/** ID of the saved PENDING invoice — used in /confirm call */
	private Long invoiceId;

	/** ICDC Number extracted from PDF e.g. ICDC037190620002339 */
	private String invoiceNumber;

	private String depotName;

	/** Invoice date extracted from PDF — e.g. 2020-06-19 */
	private LocalDate invoiceDate;

	/**
	 * Stock Received Date — defaults to today. Owner must confirm or change this to
	 * actual vehicle arrival date.
	 */
	private LocalDate stockReceivedDate;

	private Double totalAmount;
	private String pdfFileName;

	/** Retailer info extracted from ICDC header */
	private String retailerName;
	private String retailerCode;
	private String licenseNumber;

	/** All line items extracted from the invoice table */
	private List<ExtractedItemResponse> extractedItems;

	/** Summary from ICDC footer — used to validate extraction accuracy */
	private Integer summaryTotalCases;
	private Integer summaryBreakageCases;
	private Integer summaryShortageCases;

	// Extraction metadata
	private boolean extractionSuccess;
	private String extractionMessage;
	private int totalLinesExtracted;

	/** True if extracted case totals don't match ICDC summary footer */
	private boolean totalsMismatch;
}