package com.barinventory.pdf.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barinventory.pdf.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
	
	Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}