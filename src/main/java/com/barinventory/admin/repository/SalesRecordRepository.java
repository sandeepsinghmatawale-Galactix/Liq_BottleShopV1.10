package com.barinventory.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.SalesRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {
    
    List<SalesRecord> findBySessionSessionId(Long sessionId);
    
    @Query("SELECT COALESCE(SUM(s.totalRevenue), 0) FROM SalesRecord s " +
           "WHERE s.session.sessionId = :sessionId")
    BigDecimal getTotalRevenueBySession(@Param("sessionId") Long sessionId);
    
    @Query("SELECT s FROM SalesRecord s WHERE s.session.bar.barId = :barId " +
           "AND s.session.sessionStartTime BETWEEN :startDate AND :endDate " +
           "ORDER BY s.session.sessionStartTime DESC")
    List<SalesRecord> findSalesByBarAndDateRange(
        @Param("barId") Long barId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("""
    	    SELECT 
    	        bs.id,
    	        b.brandName,
    	        bs.sizeLabel,
    	        SUM(s.quantitySold),
    	        SUM(s.totalRevenue)
    	    FROM SalesRecord s
    	    JOIN s.brandSize bs
    	    JOIN bs.brand b
    	    WHERE s.session.bar.barId = :barId
    	      AND s.session.sessionStartTime BETWEEN :startDate AND :endDate
    	    GROUP BY bs.id, b.brandName, bs.sizeLabel
    	""")
    	List<Object[]> getProductSummaryRaw(
    	    @Param("barId") Long barId,
    	    @Param("startDate") LocalDateTime startDate,
    	    @Param("endDate") LocalDateTime endDate
    	);
    
}
