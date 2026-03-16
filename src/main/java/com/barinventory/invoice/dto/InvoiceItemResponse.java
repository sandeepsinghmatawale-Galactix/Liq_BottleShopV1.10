package com.barinventory.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO — single line item within an invoice response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponse {

	private Long id;

	/** Brand name exactly as printed on ICDC PDF */
	private String brandNameRaw;

	/** Brand name after fuzzy match / owner confirmation against master */
	private String brandNameMatched;

	private Long brandMasterId;

	/** Bottle size in ml — e.g. 650, 375, 180, 90 */
	private Integer sizeMl;

	/** Number of cases as printed on invoice */
	private Integer invoicedCases;

	/** Bottles per case — from Pack Qty/Size column e.g. 12, 24, 48, 96 */
	private Integer bottlesPerCase;

	/** Total invoiced bottles = invoicedCases × bottlesPerCase */
	private Integer invoicedBottles;

	/** Actual cases received after physical count */
	private Integer receivedCases;

	/** Net bottles received = (receivedCases × bottlesPerCase) - breakageQty */
	private Integer receivedBottles;

	/** Broken/damaged bottles found on arrival */
	private Integer breakageQty;

	/** Shortage = invoicedCases - receivedCases */
	private Integer shortageCases;

	private Double mrpPerBottle;
	private Double ratePerCase;
	private Double lineTotal;
	private Boolean postedToStockroom;

	private boolean hasShortage;
	private boolean hasBreakage;

	/** Product type from ICDC — "Beer" or "IML" */
	private String productType;

	/** Pack type from ICDC — "G" (Glass) or "P" (PET) */
	private String packType;
}