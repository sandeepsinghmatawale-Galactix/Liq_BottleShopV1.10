package com.barinventory.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.Product;
import com.barinventory.admin.entity.StockroomInventory;
import com.barinventory.brands.entity.BrandSize;

import jakarta.transaction.Transactional;

@Repository
public interface StockroomInventoryRepository extends JpaRepository<StockroomInventory, Long> {

	List<StockroomInventory> findBySession_SessionId(Long sessionId);

	 
	@Modifying
	@Transactional
	void deleteBySession_SessionId(Long sessionId);

	 

	long countBySession_SessionId(Long sessionId);

	// Find all stockroom entries by session
	// --------------------------
	List<StockroomInventory> findBySessionSessionId(Long sessionId);

	 
	@Modifying
	@Transactional
	void deleteBySessionSessionId(Long sessionId);

	long countBySessionSessionId(Long sessionId);

 
	// Optional but useful - Find specific brand in session
	@Query("SELECT s FROM StockroomInventory s WHERE s.session.sessionId = :sessionId "
			+ "AND s.brandSize.id = :brandSizeId")
	Optional<StockroomInventory> findBySessionAndBrandSize(@Param("sessionId") Long sessionId,
			@Param("brandSizeId") Long brandSizeId);

	List<StockroomInventory> findBySession(InventorySession session);

 
	Optional<StockroomInventory> findBySessionAndBrandSize(InventorySession session, BrandSize brandSize);
 
	Optional<StockroomInventory> findBySessionSessionIdAndBrandSizeId(Long sessionId, Long brandSizeId);

}