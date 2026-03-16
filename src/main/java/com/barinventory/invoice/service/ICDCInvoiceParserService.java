package com.barinventory.invoice.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.barinventory.invoice.dto.ExtractedInvoiceData;
import com.barinventory.invoice.dto.ExtractedItemData;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ICDCInvoiceParserService implements InvoiceParserService {

    // ── Header Patterns ───────────────────────────────────────────────────────

    private static final Pattern ICDC_NUMBER_PATTERN = Pattern.compile(
            "ICDC\\s*Number\\s*[:\\s]+(ICDC[A-Z0-9]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern INVOICE_DATE_PATTERN = Pattern.compile(
            "Invoice\\s*Date\\s*[:\\s]+(\\d{1,2}-[A-Za-z]+-\\d{4})");

    private static final Pattern DEPOT_NAME_PATTERN = Pattern.compile(
            "IML\\s*DEPOT\\s*[:\\s]+(.+?)(?:\\n|\\r|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RETAILER_NAME_PATTERN = Pattern.compile(
            "Name\\s*[:\\s]+([A-Za-z\\s]+?)\\s+(?:Code|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RETAILER_CODE_PATTERN = Pattern.compile(
            "Code\\s*[:\\s]+(\\d{5,10})");

    private static final Pattern LICENSE_NO_PATTERN = Pattern.compile(
            "License\\s*No\\s*[:\\s]+([A-Z0-9]+)");

    private static final Pattern NET_INVOICE_VALUE_PATTERN = Pattern.compile(
            "Net\\s*Invoice\\s*Value\\s*[:\\s]*([\\d,]+\\.?\\d*)");

    // ── Line Item Pattern ─────────────────────────────────────────────────────

    private static final Pattern LINE_ITEM_PATTERN = Pattern.compile(
            "^(\\d{1,2})\\s+"                                     // Sl.No
          + "(\\d{4})\\s+"                                         // Brand Number
          + "(.+?)\\s+"                                            // Brand Name
          + "(Beer|IML)\\s+"                                       // Product Type
          + "([GP])\\s+"                                           // Pack Type
          + "(\\d{1,3})\\s*/\\s*(\\d{2,4})\\s*(?:ml)?\\s+"       // Pack Qty / Size
          + "(\\d{1,4})\\s+"                                       // Cases Delivered
          + "0\\s+"                                                 // Bottles (always 0)
          + "([\\d,]+\\.\\d{2})\\s*/\\s*([\\d,]+\\.\\d{2})\\s+"  // Rate/Case / UnitRate
          + "([\\d,]+\\.\\d{2})",                                  // Total
            Pattern.CASE_INSENSITIVE);

    // ── Summary Patterns ──────────────────────────────────────────────────────

    private static final Pattern SUMMARY_INVOICE_QTY_PATTERN = Pattern.compile(
            "Invoice\\s*Qty\\s+(\\d+)\\s*/\\s*\\d+\\s+(\\d+)\\s*/\\s*\\d+\\s+(\\d+)\\s*/\\s*\\d+");

    private static final Pattern BREAKAGE_QTY_PATTERN = Pattern.compile(
            "Breakage\\s*Qty\\s+(\\d+)\\s*/\\s*(\\d+)");

    private static final Pattern SHORTAGE_QTY_PATTERN = Pattern.compile(
            "Shortage\\s*Qty\\s+(\\d+)\\s*/\\s*(\\d+)");

    // ── Date Formatters ───────────────────────────────────────────────────────

    private static final DateTimeFormatter ICDC_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter FALLBACK_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH);

    // ── Main Parse Entry ──────────────────────────────────────────────────────

    @Override
    public ExtractedInvoiceData parse(String rawText) {
        log.info("Parsing ICDC invoice. Text length: {}", rawText.length());

        ExtractedInvoiceData result = new ExtractedInvoiceData();

        try {
            result.setInvoiceNumber(extractICDCNumber(rawText));
            result.setInvoiceDate(extractInvoiceDate(rawText));
            result.setDepotName(extractDepotName(rawText));
            result.setRetailerName(extractRetailerName(rawText));
            result.setRetailerCode(extractRetailerCode(rawText));
            result.setLicenseNumber(extractLicenseNumber(rawText));
            result.setTotalAmount(extractNetInvoiceValue(rawText));

            List<ExtractedItemData> items = extractLineItems(rawText);
            result.setItems(items);

            enrichWithSummaryData(result, rawText);

            // Validate totals — flag mismatch for owner review
            boolean totalsMatch = validateTotals(result);
            result.setTotalsMismatch(!totalsMatch);

            result.setExtractionSuccess(true);
            result.setTotalLinesExtracted(items.size());

            // Flag for manual review if nothing extracted
            if (items.isEmpty()) {
                result.setRequiresManualReview(true);
                result.setManualReviewReason(
                        "No line items found — PDF may have unusual table formatting.");
            }

            log.info("ICDC parse complete. Invoice#: {}, Date: {}, Depot: {}, Items: {}",
                    result.getInvoiceNumber(), result.getInvoiceDate(),
                    result.getDepotName(), items.size());

        } catch (Exception e) {
            log.error("ICDC parse failed: {}", e.getMessage(), e);
            result.setExtractionSuccess(false);
            result.setExtractionMessage("ICDC parse error: " + e.getMessage());
        }

        return result;
    }

    // ── Header Extractors ─────────────────────────────────────────────────────

    private String extractICDCNumber(String text) {
        Matcher m = ICDC_NUMBER_PATTERN.matcher(text);
        if (m.find()) return m.group(1).trim();
        log.warn("ICDC Number not found");
        return null;
    }

    private LocalDate extractInvoiceDate(String text) {
        Matcher m = INVOICE_DATE_PATTERN.matcher(text);
        if (m.find()) {
            String dateStr = m.group(1).trim();
            try {
                return LocalDate.parse(dateStr, ICDC_DATE_FORMAT);
            } catch (DateTimeParseException e1) {
                try {
                    return LocalDate.parse(dateStr, FALLBACK_DATE_FORMAT);
                } catch (DateTimeParseException e2) {
                    log.warn("Could not parse ICDC date: {}", dateStr);
                }
            }
        }
        return null;
    }

    private String extractDepotName(String text) {
        Matcher m = DEPOT_NAME_PATTERN.matcher(text);
        if (m.find()) return m.group(1).trim();
        if (text.contains("Ranga Reddy")) return "IMFL Depot Ranga Reddy";
        if (text.contains("IMFL Depot"))  return "IMFL Depot";
        return "Telangana ICDC Depot";
    }

    private String extractRetailerName(String text) {
        Matcher m = RETAILER_NAME_PATTERN.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractRetailerCode(String text) {
        Matcher m = RETAILER_CODE_PATTERN.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractLicenseNumber(String text) {
        Matcher m = LICENSE_NO_PATTERN.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private Double extractNetInvoiceValue(String text) {
        Matcher m = NET_INVOICE_VALUE_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", "").trim());
            } catch (NumberFormatException e) {
                log.warn("Could not parse net invoice value");
            }
        }
        return null;
    }

    // ── Line Item Extraction ──────────────────────────────────────────────────

    private List<ExtractedItemData> extractLineItems(String text) {
        List<ExtractedItemData> items = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            ExtractedItemData item = tryParseICDCItemLine(line);
            if (item != null) {
                item.calculateTotals();
                items.add(item);
                log.debug("Item: {} | {}ml | {} cases | {} btl/case",
                        item.getBrandNameRaw(), item.getSizeMl(),
                        item.getInvoicedCases(), item.getBottlesPerCase());
            }
        }

        if (items.isEmpty()) {
            log.warn("Standard parse yielded 0 items — trying multi-line fallback");
            items = extractItemsMultiLine(text);
        }

        return items;
    }

    private ExtractedItemData tryParseICDCItemLine(String line) {
        Matcher m = LINE_ITEM_PATTERN.matcher(line);
        if (!m.find()) return null;

        try {
            ExtractedItemData item = new ExtractedItemData();
            item.setBrandNameRaw(m.group(3).trim());
            item.setProductType(m.group(4).trim());
            item.setPackType(m.group(5).trim());
            item.setBottlesPerCase(Integer.parseInt(m.group(6)));
            item.setSizeMl(Integer.parseInt(m.group(7)));
            item.setInvoicedCases(Integer.parseInt(m.group(8)));
            item.setRatePerCase(Double.parseDouble(m.group(9).replace(",", "")));
            item.setMrpPerBottle(Double.parseDouble(m.group(10).replace(",", "")));
            item.setLineTotal(Double.parseDouble(m.group(11).replace(",", "")));
            return item;
        } catch (NumberFormatException e) {
            log.debug("Could not parse line: {}", line.substring(0, Math.min(60, line.length())));
            return null;
        }
    }

    private List<ExtractedItemData> extractItemsMultiLine(String text) {
        List<ExtractedItemData> items = new ArrayList<>();
        // Join continuation lines (brand names that wrap) into single lines
        String normalized = text.replaceAll("\\n([^0-9\\r\\n])", " $1");
        for (String line : normalized.split("\\r?\\n")) {
            ExtractedItemData item = tryParseICDCItemLine(line.trim());
            if (item != null) {
                item.calculateTotals();
                items.add(item);
            }
        }
        log.info("Multi-line fallback found {} items", items.size());
        return items;
    }

    // ── Summary / Footer ──────────────────────────────────────────────────────

    private void enrichWithSummaryData(ExtractedInvoiceData result, String text) {
        Matcher mqty = SUMMARY_INVOICE_QTY_PATTERN.matcher(text);
        if (mqty.find()) {
            result.setSummaryTotalCases(Integer.parseInt(mqty.group(3)));
            log.debug("Summary total cases: {}", result.getSummaryTotalCases());
        }

        Matcher mb = BREAKAGE_QTY_PATTERN.matcher(text);
        if (mb.find()) result.setSummaryBreakageCases(Integer.parseInt(mb.group(1)));

        Matcher ms = SHORTAGE_QTY_PATTERN.matcher(text);
        if (ms.find()) result.setSummaryShortageCases(Integer.parseInt(ms.group(1)));
    }

    public boolean validateTotals(ExtractedInvoiceData data) {
        if (data.getSummaryTotalCases() == null || data.getItems() == null) return true;

        int extractedTotal = data.getItems().stream()
                .mapToInt(i -> i.getInvoicedCases() != null ? i.getInvoicedCases() : 0).sum();

        boolean matches = extractedTotal == data.getSummaryTotalCases();
        if (!matches) {
            log.warn("Cases mismatch — extracted: {}, ICDC footer: {}",
                    extractedTotal, data.getSummaryTotalCases());
        }
        return matches;
    }
}