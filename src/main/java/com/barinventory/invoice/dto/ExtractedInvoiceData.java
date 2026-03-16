package com.barinventory.invoice.dto;

 

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO — populated by ICDCInvoiceParserService after PDFBox extraction.
 * Passed to InvoiceService for saving and building ExtractionResultResponse.
 * NOT returned directly to frontend — ExtractionResultResponse is the frontend DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedInvoiceData {

    // ── Extracted from PDF header ─────────────────────────────────────────────

    /** ICDC Number e.g. ICDC037190620002339 */
    private String invoiceNumber;

    /** Date printed on govt depot invoice e.g. 2020-06-19 */
    private LocalDate invoiceDate;

    private String depotName;
    private String depotCode;

    /** Retailer (bar) name from ICDC header */
    private String retailerName;

    /** Retailer code from ICDC header */
    private String retailerCode;

    /** Bar license number from ICDC header */
    private String licenseNumber;

    /** Net Invoice Value from ICDC footer */
    private Double totalAmount;

    // ── Line items extracted from invoice table ───────────────────────────────

    private List<ExtractedItemData> items;

    // ── Summary from ICDC footer ──────────────────────────────────────────────

    private Integer summaryTotalCases;
    private Integer summaryBreakageCases;
    private Integer summaryShortageCases;

    // ── Extraction metadata ───────────────────────────────────────────────────

    private boolean extractionSuccess;
    private String extractionMessage;
    private int totalLinesExtracted;

    /** True if extracted item case totals don't match ICDC footer summary */
    private boolean totalsMismatch;

    /** True if parser could not extract cleanly — owner must review manually */
    private boolean requiresManualReview;

    /** Reason shown to owner why manual review is needed */
    private String manualReviewReason;

    /** Raw text from PDFBox — kept for debugging only, not sent to frontend */
    private String rawExtractedText;
}