package com.barinventory.invoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.invoice.entity.InvoiceItem;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

	// ── All items for a given invoice ─────────────────────────────────────────

	List<InvoiceItem> findByInvoiceId(Long invoiceId);

	// ── Items not yet posted to stockroom ─────────────────────────────────────
	// Used for retry/recovery if stockroom posting failed mid-way

	@Query("SELECT i FROM InvoiceItem i " + "WHERE i.invoice.id = :invoiceId " + "AND i.postedToStockroom = false")
	List<InvoiceItem> findUnpostedByInvoiceId(@Param("invoiceId") Long invoiceId);

	// ── All items for a brand — stock history per brand ───────────────────────

	List<InvoiceItem> findByBrandMasterId(Long brandMasterId);

	// ── Items with breakage — for damage report ───────────────────────────────

	@Query("SELECT i FROM InvoiceItem i " + "WHERE i.invoice.id = :invoiceId " + "AND i.breakageQty > 0")
	List<InvoiceItem> findBreakageItemsByInvoiceId(@Param("invoiceId") Long invoiceId);

	// ── Items with shortage — for depot claims ────────────────────────────────

	@Query("SELECT i FROM InvoiceItem i " + "WHERE i.invoice.id = :invoiceId " + "AND i.shortageCases > 0")
	List<InvoiceItem> findShortageItemsByInvoiceId(@Param("invoiceId") Long invoiceId);
}