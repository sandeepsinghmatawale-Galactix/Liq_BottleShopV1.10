package com.barinventory.invoice.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Stores uploaded invoice PDFs on the server filesystem. Files organised by
 * year/month: uploads/invoices/2024/03/uuid_originalname.pdf
 */
@Service
@Slf4j
public class InvoiceFileStorageService {

	@Value("${app.upload.dir:uploads}")
	private String uploadBaseDir;

	/**
	 * Stores uploaded PDF and returns stored file path. UUID prefix prevents
	 * filename collisions.
	 */
	public String storeFile(MultipartFile file) {
		try {
			// Build directory: uploads/invoices/2024/03/
			String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
			Path uploadDir = Paths.get(uploadBaseDir, "invoices", yearMonth);
			Files.createDirectories(uploadDir);

			// Sanitise original filename + add UUID prefix
			String originalName = file.getOriginalFilename() != null
					? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
					: "invoice.pdf";
			String uniqueName = UUID.randomUUID() + "_" + originalName;

			Path target = uploadDir.resolve(uniqueName);
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

			log.info("Invoice PDF stored: {}", target);
			return target.toString();

		} catch (IOException e) {
			log.error("Failed to store PDF: {}", e.getMessage());
			throw new RuntimeException("Could not store invoice file: " + e.getMessage(), e);
		}
	}

	/**
	 * Loads stored PDF as byte array. Used by InvoiceService.getInvoicePdf() for
	 * the /pdf download endpoint. Frontend PDF.js renders the returned bytes
	 * inline.
	 */
	public byte[] loadFile(String filePath) throws IOException {
		Path path = Paths.get(filePath);
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Invoice PDF file not found: " + filePath);
		}
		return Files.readAllBytes(path);
	}

	/**
	 * Deletes stored file. Called by InvoiceService when PDF extraction fails — no
	 * point keeping a file whose invoice was never saved.
	 */
	public void deleteFile(String filePath) {
		try {
			if (filePath != null) {
				Files.deleteIfExists(Paths.get(filePath));
				log.info("Invoice PDF deleted: {}", filePath);
			}
		} catch (IOException e) {
			log.warn("Could not delete invoice PDF: {}", filePath);
		}
	}
}