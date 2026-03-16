package com.barinventory.invoice.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO — single extracted line item shown on the review screen. Owner
 * can edit receivedCases, breakageQty and confirm brandNameMatched.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedItemResponse {

	/** DB id of the saved InvoiceItem — used when confirming */
	private Long itemId;

	/** Brand name exactly as read from ICDC PDF */
	private String brandNameRaw;

	/** Best fuzzy match from your brand master list */
	private String brandNameMatched;

	private Long brandMasterId;

	/**
	 * True if fuzzy match confidence >= 85%. False = owner must manually confirm
	 * the brand mapping.
	 */
	private boolean matchConfident;

	/** Bottle size in ml */
	private Integer sizeMl;

	/** Cases as printed on invoice */
	private Integer invoicedCases;

	/** Bottles per case from Pack Qty/Size column */
	private Integer bottlesPerCase;

	/** invoicedCases × bottlesPerCase */
	private Integer invoicedBottles;

	private Double mrpPerBottle;
	private Double ratePerCase;
	private Double lineTotal;

	/** Product type from ICDC — "Beer" or "IML" */
	private String productType;

	/** Pack type — "G" (Glass) or "P" (PET) */
	private String packType;
}