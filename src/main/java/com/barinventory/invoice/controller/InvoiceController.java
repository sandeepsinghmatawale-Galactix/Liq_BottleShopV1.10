package com.barinventory.invoice.controller;
 

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.barinventory.invoice.dto.*;
import com.barinventory.invoice.service.InvoiceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ✅ Get All Invoices (API)
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceSummaryResponse>>> getAllInvoices() {
        List<InvoiceSummaryResponse> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(ApiResponse.success(invoices.size() + " invoice(s) found.", invoices));
    }

    // ✅ Get Pending
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<InvoiceSummaryResponse>>> getPendingInvoices() {
        List<InvoiceSummaryResponse> pending = invoiceService.getPendingInvoices();
        return ResponseEntity.ok(ApiResponse.success(pending.size() + " pending.", pending));
    }

    // ✅ Get By ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoiceById(id)));
    }

    // ✅ Date Range
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<InvoiceSummaryResponse>>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("'from' must be <= 'to'"));
        }

        List<InvoiceSummaryResponse> invoices =
                invoiceService.getInvoicesByDateRange(from, to);

        return ResponseEntity.ok(ApiResponse.success("Filtered invoices", invoices));
    }

    // ✅ PDF
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        byte[] pdf = invoiceService.getInvoicePdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=invoice-" + id + ".pdf")
                .body(pdf);
    }
}