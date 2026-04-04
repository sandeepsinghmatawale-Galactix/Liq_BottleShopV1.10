package com.barinventory.admin.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.DistributionRecord;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.brands.entity.BrandSize;

import jakarta.transaction.Transactional;

@Repository
public interface DistributionRecordRepository extends JpaRepository<DistributionRecord, Long> {
     
   
    Optional<DistributionRecord> findBySessionAndBrandSize(
            InventorySession session, BrandSize brandSize);
    
       @Modifying
    @Transactional
    @Query("""
        UPDATE DistributionRecord dr
        SET dr.quantityFromStockroom = :qty,
            dr.totalAllocated = 0,
            dr.unallocated = :qty,
            dr.status = 'PENDING_ALLOCATION'
        WHERE dr.session.sessionId = :sessionId
          AND dr.brandSize.id = :brandSizeId
    """)
    int updateDistributionBulk(   // ✅ MUST BE int
        @Param("sessionId") Long sessionId,
        @Param("brandSizeId") Long brandSizeId,
        @Param("qty") BigDecimal qty
    );
        

    // EXISTING METHODS
    Optional<DistributionRecord> findById(Long id);
    
    /**
     * Find all distributions for a session.
     * THIS METHOD MUST EXIST
     */
    @Query("SELECT d FROM DistributionRecord d WHERE d.session.sessionId = :sessionId")
    List<DistributionRecord> findBySessionSessionId(@Param("sessionId") Long sessionId);
    
    /**
     * Find distribution by session and brand size.
     * THIS METHOD MUST EXIST
     */
    @Query("SELECT d FROM DistributionRecord d WHERE d.session.sessionId = :sessionId " +
           "AND d.brandSize.id = :brandSizeId")
    Optional<DistributionRecord> findBySessionSessionIdAndBrandSizeId(
            @Param("sessionId") Long sessionId,
            @Param("brandSizeId") Long brandSizeId);
    
    
 
 


    
}
