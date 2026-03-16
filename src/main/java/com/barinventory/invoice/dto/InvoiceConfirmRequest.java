package com.barinventory.invoice.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO — sent by frontend when owner confirms extracted invoice data.
 * Owner enters actual received quantities, breakage, and stock received date.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceConfirmRequest {

	private Long invoiceId;

	/**
	 * Stock Received Date — actual date vehicle arrived and stock was unloaded.
	 * Entered manually by bar owner on the review screen. Can differ from
	 * invoiceDate if vehicle was delayed.
	 */
	private LocalDate stockReceivedDate;

	private Double vehicleCharges;
	private String vehicleNumber;
	private String remarks;

	private List<InvoiceItemConfirmRequest> items;
}