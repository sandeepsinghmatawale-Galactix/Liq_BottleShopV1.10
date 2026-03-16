package com.barinventory.invoice.dto;

 

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.barinventory.invoice.entity.InvoiceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO — lightweight summary for the invoice list screen. Does not
 * include line items — use InvoiceResponse for full detail.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceSummaryResponse {

	private Long id;
	private String invoiceNumber;
	private String depotName;

	/** Date printed on govt depot invoice */
	private LocalDate invoiceDate;

	/** Actual date stock was received at bar premises */
	private LocalDate stockReceivedDate;

	private InvoiceStatus status;
	private int totalItems;
	private int totalCases;
	private boolean hasDiscrepancy;
	private LocalDateTime uploadedAt;
}
