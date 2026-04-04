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
    // BASIC FINDERS
    // --------------------------

    List<WellInventory> findBySessionSessionId(Long sessionId);

    List<WellInventory> findBySessionSessionIdAndBrandSizeId(Long sessionId, Long brandSizeId);

    List<WellInventory> findBySessionSessionIdAndBrandSize(Long sessionId, BrandSize brandSize);

    List<WellInventory> findBySessionSessionIdAndBarWell_Id(Long sessionId, Long barWellId);

    List<WellInventory> findBySessionSessionIdAndBarWellAndBrandSize(Long sessionId, BarWell barWell, BrandSize brandSize);

    // --------------------------
    // OPTIONAL SINGLE RECORD
    // --------------------------

    Optional<WellInventory> findBySessionAndBarWellAndBrandSize(
            InventorySession session,
            BarWell barWell,
            BrandSize brandSize
    );

    // ✅ BEST METHOD (Replaces broken custom query)
    Optional<WellInventory> findBySession_SessionIdAndBarWell_Id(Long sessionId, Long barWellId);

    // --------------------------
    // AGGREGATE SUMS
    // --------------------------

    @Query("SELECT COALESCE(SUM(w.receivedFromDistribution), 0) " +
           "FROM WellInventory w " +
           "WHERE w.session.sessionId = :sessionId " +
           "AND w.brandSize.id = :brandSizeId")
    BigDecimal sumReceivedBySessionAndBrandSize(
            @Param("sessionId") Long sessionId,
            @Param("brandSizeId") Long brandSizeId
    );

    @Query("SELECT COALESCE(SUM(w.consumed), 0) " +
           "FROM WellInventory w " +
           "WHERE w.session.sessionId = :sessionId " +
           "AND w.brandSize.id = :brandSizeId")
    BigDecimal sumConsumedBySessionAndBrandSize(
            @Param("sessionId") Long sessionId,
            @Param("brandSizeId") Long brandSizeId
    );

    // --------------------------
    // DELETE & COUNT
    // --------------------------

    @Modifying
    @Transactional
    void deleteBySessionSessionId(Long sessionId);

    long countBySessionSessionId(Long sessionId);
}