package com.barinventory.admin.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.entity.BarWell;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.WellInventory;
import com.barinventory.brands.entity.BrandSize;

@Repository
public interface WellInventoryRepository extends JpaRepository<WellInventory, Long> {

	// --------------------------
	// Find by session
	// --------------------------
	List<WellInventory> findBySessionSessionId(Long sessionId);

	List<WellInventory> findBySessionSessionIdAndBrandSizeId(Long sessionId, Long brandSizeId);

	List<WellInventory> findBySessionSessionIdAndProductProductId(Long sessionId, Long productId);

	List<WellInventory> findBySessionSessionIdAndBrandSize(Long sessionId, BrandSize brandSize);

	List<WellInventory> findBySessionId(Long sessionId);

	List<WellInventory> findBySessionIdAndBrandSizeId(Long sessionId, Long brandSizeId);

	List<WellInventory> findBySessionIdAndBrandSize(Long sessionId, BrandSize brandSize);

	// --------------------------
	// Aggregate sums
	// --------------------------

	@Query("SELECT COALESCE(SUM(w.receivedFromDistribution), 0) " + "FROM WellInventory w "
			+ "WHERE w.session.sessionId = :sessionId AND w.product.productId = :productId")
	BigDecimal sumReceivedBySessionAndProduct(@Param("sessionId") Long sessionId, @Param("productId") Long productId);

	@Query("SELECT COALESCE(SUM(w.consumed), 0) " + "FROM WellInventory w "
			+ "WHERE w.session.sessionId = :sessionId AND w.product.productId = :productId")
	BigDecimal sumConsumedBySessionAndProduct(@Param("sessionId") Long sessionId, @Param("productId") Long productId);

	@Query("SELECT COALESCE(SUM(w.receivedFromDistribution), 0) " + "FROM WellInventory w "
			+ "WHERE w.session.id = :sessionId AND w.brandSize.id = :brandSizeId")
	BigDecimal sumReceivedBySessionAndBrandSize(@Param("sessionId") Long sessionId,
			@Param("brandSizeId") Long brandSizeId);

	@Query("SELECT COALESCE(SUM(w.consumed), 0) " + "FROM WellInventory w "
			+ "WHERE w.session.id = :sessionId AND w.brandSize.id = :brandSizeId")
	BigDecimal sumConsumedBySessionAndBrandSize(@Param("sessionId") Long sessionId,
			@Param("brandSizeId") Long brandSizeId);

	// --------------------------
	// Delete & count
	// --------------------------
	@Modifying
	@Transactional
	void deleteBySessionSessionId(Long sessionId);

	long countBySessionSessionId(Long sessionId);

	@Modifying
	@Transactional
	void deleteBySessionId(Long sessionId);

	long countBySessionId(Long sessionId);

	// --------------------------
	// Optional finder by session, bar well, and brand size
	// --------------------------
	Optional<WellInventory> findBySessionAndBarWellAndBrandSize(InventorySession session, BarWell barWell,
			BrandSize brandSize);
}