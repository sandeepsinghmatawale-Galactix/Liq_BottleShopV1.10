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

	@PostMapping("/upload")
	public String uploadInvoices(@RequestParam("files") MultipartFile[] files, Model model) {
	    if (files == null || files.length == 0 || files[0].isEmpty()) {
	        model.addAttribute("error", "No files selected");
	        return "invoices/invoice-upload";
	    }

	    try {
	        // Process the first file and add result to model
	        Invoice invoice = service.processInvoice(files[0]);
	        model.addAttribute("invoice", invoice);  // ← THIS was missing
	        return "invoices/invoice-view";

	    } catch (Exception e) {
	        model.addAttribute("error", "Failed to process invoice: " + e.getMessage());
	        return "invoices/invoice-upload";
	    }
	}

}