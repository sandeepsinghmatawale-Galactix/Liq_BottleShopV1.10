package com.barinventory.pdf.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barinventory.pdf.entity.InvoiceItem;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
}