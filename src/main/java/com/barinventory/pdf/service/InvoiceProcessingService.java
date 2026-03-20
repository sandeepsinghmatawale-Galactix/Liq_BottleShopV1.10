package com.barinventory.pdf.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.barinventory.pdf.entity.Invoice;
import com.barinventory.pdf.entity.InvoiceItem;
import com.barinventory.pdf.repository.InvoiceRepository;

import lombok.extern.slf4j.Slf4j;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

@Service
@Slf4j
public class InvoiceProcessingService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    // ================= MAIN =================
    public Invoice processInvoice(MultipartFile file) {
        try {
            String text = extractText(file);
            List<List<String>> table = extractTable(file);

            log.info("==== RAW TEXT ====\n{}", text);

            Invoice invoice = parseInvoice(text, table);

            return invoiceRepository.save(invoice);

        } catch (Exception e) {
            throw new RuntimeException("Processing failed", e);
        }
    }

    // ================= TEXT =================
    private String extractText(MultipartFile file) throws Exception {
        Tika tika = new Tika();
        try (InputStream is = file.getInputStream()) {
            return tika.parseToString(is);
        }
    }

    // ================= TABLE =================
    private List<List<String>> extractTable(MultipartFile file) throws Exception {

        List<List<String>> tableData = new ArrayList<>();

        try (InputStream is = file.getInputStream(); PDDocument doc = PDDocument.load(is)) {

            ObjectExtractor extractor = new ObjectExtractor(doc);
            PageIterator pages = extractor.extract();

            while (pages.hasNext()) {
                Page page = pages.next();

                SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                List<Table> tables = sea.extract(page);

                if (tables.isEmpty()) {
                    tables = new BasicExtractionAlgorithm().extract(page);
                }

                for (Table table : tables) {
                    for (List<RectangularTextContainer> row : table.getRows()) {

                        List<String> rowData = new ArrayList<>();

                        for (RectangularTextContainer cell : row) {
                            rowData.add(cell.getText().trim());
                        }

                        if (!rowData.isEmpty()) {
                            tableData.add(rowData);
                        }
                    }
                }
            }
        }

        return tableData;
    }

    // ================= PARSER =================
    private Invoice parseInvoice(String text, List<List<String>> table) {

        Invoice invoice = new Invoice();

        // 🔹 BASIC
        invoice.setInvoiceNumber(extractAfter(text, "ICDC Number"));
        invoice.setRetailerName(extractAfter(text, "Name:"));
        invoice.setRetailerCode(extractAfter(text, "Code:"));
        invoice.setAddress(extractAfter(text, "Address"));
        invoice.setLicenseNo(extractAfter(text, "License No"));

        String licenseLine = extractAfter(text, "Name / Phone");
        if (licenseLine.contains("/")) {
            String[] parts = licenseLine.split("/");
            invoice.setLicenseName(parts[0].trim());
            invoice.setLicensePhone(parts.length > 1 ? parts[1].trim() : "");
        }

        invoice.setInvoiceDate(LocalDate.now());

        // ================= FINANCIAL =================

     // ================= FINANCIAL =================

     // Invoice Value
     invoice.setInvoiceValue(
             extractAmountRegex(text, "invoice value")
     );

     // ✅ MRP Rounding Off (STRICT)
     invoice.setMrpRounding(
             extractAmountByLine(text, "MRP Rounding Off")
     );

     // ✅ Net Invoice Value (STRICT)
     invoice.setTotalAmount(
             extractAmountByLine(text, "Net Invoice Value")
     );

     // ✅ E-Challan / DD Amount (FIXED)
     invoice.setEchallan(
             extractAmountByLine(text, "E-Challan / DD Amount")
     );

     // Others remain SAME
     invoice.setPreviousCredit(
             extractAmountRegex(text, "previous credit")
     );

     invoice.setSubtotal(
             extractAmountRegex(text, "sub total", "subtotal")
     );

     invoice.setLessInvoice(
             extractAmountRegex(text, "less this invoice value", "less invoice")
     );

     invoice.setSpecialCess(
             extractAmountRegex(text, "special excise cess", "special cess")
     );

     invoice.setTcs(
             extractAmountRegex(text, "tcs")
     );

     // ✅ Retailer Credit Balance (STRICT)
     invoice.setRetailerCreditBalance(
             extractAmountByLine(text, "Retailer Credit Balance")
     );

        // ================= CASE SUMMARY =================
        extractCaseSummary(text, invoice);

        // ================= ITEMS =================
        List<InvoiceItem> items = new ArrayList<>();

        for (List<String> row : table) {
            try {
                if (row.size() < 10) continue;
                if (!row.get(0).matches("\\d+")) continue;

                InvoiceItem item = new InvoiceItem();

                item.setBrandName(row.get(2));
                item.setCategory(row.get(3));
                item.setPack(row.get(5));

                item.setCasesQty(parseInt(row.get(6)));
                item.setBottlesQty(parseInt(row.get(7)));

                item.setRate(parseDouble(row.get(8)));
                item.setTotal(parseDouble(row.get(9)));

                item.setInvoice(invoice);
                items.add(item);

            } catch (Exception e) {
                log.warn("Skipping row {}", row);
            }
        }

        invoice.setItems(items);
        return invoice;
    }

    // ================= LINE-BASED FIX =================
    private Double extractAmountByLine(String text, String key) {
        try {
            String lowerText = text.toLowerCase();
            int index = lowerText.indexOf(key.toLowerCase());

            if (index == -1) return 0.0;

            // 🔥 Take BIGGER chunk (fix)
            String sub = text.substring(index, Math.min(index + 300, text.length()));

            // 🔥 Allow newline, spaces, colon etc.
            Matcher m = Pattern.compile(key + "[^0-9]*([0-9,]+\\.?[0-9]*)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                    .matcher(sub);

            if (m.find()) {
                return Double.parseDouble(m.group(1).replace(",", ""));
            }

        } catch (Exception e) {
            log.warn("Failed extracting for key {}", key);
        }

        return 0.0;
    }

    // ================= REGEX =================
    private Double extractAmountRegex(String text, String... keys) {

        try {
            String normalized = text.toLowerCase().replaceAll("\\s+", " ");

            for (String key : keys) {

                String pattern = key.toLowerCase().replaceAll("\\s+", "\\\\s*")
                        + "\\s*[:₹-]?\\s*([0-9,]+\\.?[0-9]*)";

                Matcher m = Pattern.compile(pattern).matcher(normalized);

                if (m.find()) {
                    return Double.parseDouble(m.group(1).replace(",", ""));
                }
            }

        } catch (Exception e) {
            log.warn("Regex fail {}", (Object) keys);
        }

        return 0.0;
    }

    // ================= HELPERS =================

    private String extractAfter(String text, String key) {
        try {
            int i = text.indexOf(key);
            if (i == -1) return "";
            return text.substring(i + key.length()).split("\n")[0].trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void extractCaseSummary(String text, Invoice invoice) {
        try {
            String line = text.substring(text.indexOf("Invoice Qty")).split("\n")[0];
            String[] p = line.replace("Invoice Qty", "").trim().split("\\s+");

            invoice.setImflCases(parseInt(p[0]));
            invoice.setImflBottles(parseInt(p[2]));
            invoice.setBeerCases(parseInt(p[3]));
            invoice.setBeerBottles(parseInt(p[5]));
            invoice.setTotalCases(parseInt(p[6]));
            invoice.setTotalBottles(parseInt(p[8]));

        } catch (Exception e) {
            log.warn("Case summary failed");
        }
    }

    private Integer parseInt(String v) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 0; }
    }

    private Double parseDouble(String v) {
        try { return Double.parseDouble(v.replace(",", "").trim()); } catch (Exception e) { return 0.0; }
    }
    
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    // Optional
    public int getPendingCount() {
        return 0; // or your logic
    }
    
    
    
}