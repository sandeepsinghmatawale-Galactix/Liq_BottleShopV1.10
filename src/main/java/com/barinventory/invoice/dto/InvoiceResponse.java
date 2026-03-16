package com.barinventory.invoice.dto;

 

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.barinventory.invoice.entity.InvoiceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO — full invoice detail. Used for the review screen and invoice
 * detail view.
 *
 * Always returns all THREE dates separately: invoiceDate → extracted from PDF
 * (govt depot bill date) stockReceivedDate → entered by owner (actual vehicle
 * arrival date) uploadedAt → system auto-captured (audit only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

	private Long id;
	private String invoiceNumber;
	private String depotName;
	private String depotCode;

	/** Date printed on the govt depot invoice — extracted from PDF */
	private LocalDate invoiceDate;

	/**
	 * Actual date vehicle arrived and stock was physically unloaded — entered by
	 * owner
	 */
	private LocalDate stockReceivedDate;

	/** System timestamp when PDF was uploaded — auto captured for audit */
	private LocalDateTime uploadedAt;

	private Double totalAmount;
	private Double vehicleCharges;
	private String vehicleNumber;
	private String pdfFileName;
	private InvoiceStatus status;
	private String remarks;
	private String uploadedBy;
	private String confirmedBy;
	private LocalDateTime confirmedAt;

	private List<InvoiceItemResponse> items;

	// Computed summary fields
	private boolean hasDiscrepancy;
	private int totalInvoicedCases;
	private int totalReceivedCases;
	private int totalBreakage;
}