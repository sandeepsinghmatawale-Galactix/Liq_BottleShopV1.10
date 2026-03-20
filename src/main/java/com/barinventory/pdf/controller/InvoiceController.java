package com.barinventory.pdf.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.barinventory.pdf.entity.Invoice;
import com.barinventory.pdf.service.InvoiceProcessingService;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceProcessingService service;

    // ✅ GET /invoices/upload
    @GetMapping("/upload")
    public String showUploadPage() {
        // Now looking inside templates/invoices/upload.html
        return "invoices/invoice-upload";
    }
    @GetMapping
    public String redirect() {
        return "redirect:/invoices/upload"; // ✅ not /invoices/invoice-upload
    }

    // ✅ POST /invoices/upload
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, Model model) {

        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a PDF file");
            return "invoices/invoice-upload";
        }

        try {
            Invoice invoice = service.processInvoice(file);
            model.addAttribute("invoice", invoice);
            // Now looking inside templates/invoices/invoice-view.html
            return "invoices/invoice-view";
        } catch (Exception e) {
            model.addAttribute("error", "Failed: " + e.getMessage());
            return "invoices/invoice-upload";
        }
    }

     
}