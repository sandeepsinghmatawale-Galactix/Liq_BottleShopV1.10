package com.barinventory.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO — one line item parsed from ICDC invoice table. Populated by
 * ICDCInvoiceParserService. After BrandMatcherService runs, brandNameMatched +
 * brandMasterId are filled. NOT returned to frontend — ExtractedItemResponse is
 * the frontend DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedItemData {

	// ── Extracted from PDF line item ──────────────────────────────────────────

	/** Brand name exactly as printed in ICDC PDF */
	private String brandNameRaw;

	/** Bottle size in ml — from Pack Qty/Size column e.g. 650, 375, 180, 90 */
	private Integer sizeMl;

	/** Bottles per case — from Pack Qty/Size column e.g. 12, 24, 48, 96 */
	private Integer bottlesPerCase;

	/** Cases as printed on invoice */
	private Integer invoicedCases;

	/** invoicedCases × bottlesPerCase — calculated by calculateTotals() */
	private Integer invoicedBottles;

	/** MRP per bottle — from Unit Rate/Btl column */
	private Double mrpPerBottle;

	/** Rate per case — from Rate/Case column */
	private Double ratePerCase;

	/** Line total value */
	private Double lineTotal;

	/** Product type from ICDC — "Beer" or "IML" */
	private String productType;

	/** Pack type from ICDC — "G" (Glass) or "P" (PET) */
	private String packType;

	// ── Filled by BrandMatcherService ────────────────────────────────────────

	/** Best fuzzy match from brand master list */
	private String brandNameMatched;

	/** Matched brand ID from master table */
	private Long brandMasterId;

	/**
	 * True if fuzzy match confidence >= 85%. False = owner must manually confirm
	 * brand mapping on review screen.
	 */
	private boolean matchConfident;

	// ── Calculated method ─────────────────────────────────────────────────────

	/**
     * Called by ICDCInvoiceParserService after setting invoicedCases + bottlesPerCase.
     * Calculates invoicedBottles and totalValue.
     */
    public void calculateTotals() {
        if (invoicedCases != null && bottlesPerCase != null) {
            this.invoicedBottles = invoicedCases * bottlesPerCase;
        }
    }
}
 