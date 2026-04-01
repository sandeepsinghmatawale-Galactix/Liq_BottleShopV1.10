package com.barinventory.admin.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.enums.SessionStatus;

@Repository
public interface InventorySessionRepository extends JpaRepository<InventorySession, Long> {

	List<InventorySession> findByBarBarIdOrderBySessionStartTimeDesc(Long barId);

	List<InventorySession> findByBarBarIdAndStatus(Long barId, SessionStatus status);

	Optional<InventorySession> findFirstByBarBarIdAndStatusOrderBySessionStartTimeDesc(Long barId,
			SessionStatus status);

	@Query("SELECT s FROM InventorySession s WHERE s.bar.barId = :barId "
			+ "AND s.sessionStartTime BETWEEN :startDate AND :endDate " + "ORDER BY s.sessionStartTime DESC")
	List<InventorySession> findSessionsByBarAndDateRange(@Param("barId") Long barId,
			@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

	@Query("SELECT s FROM InventorySession s WHERE s.status = :status " + "ORDER BY s.sessionStartTime DESC")
	List<InventorySession> findByStatus(@Param("status") SessionStatus status);

	Optional<InventorySession> findBySessionId(Long sessionId);

	@Query("SELECT s FROM InventorySession s JOIN FETCH s.bar WHERE s.id = :sessionId")
	Optional<InventorySession> findByIdWithBar(@Param("sessionId") Long sessionId);

	// FIXED — includes COMPLETED setup sessions so pre-fill works
	@Query("SELECT s FROM InventorySession s " + "WHERE s.bar.barId = :barId "
			+ "AND s.status IN (com.barinventory.admin.enums.SessionStatus.COMPLETED, "
			+ "com.barinventory.admin.enums.SessionStatus.SETUP) " + "ORDER BY s.sessionEndTime DESC")
	List<InventorySession> findCompletedSessionsByBar(@Param("barId") Long barId);
	
//	Optional<InventorySession> findTopByBar_BarIdOrderBySessionDateDesc(Long barId);
	
	Optional<InventorySession> findTopByBar_BarIdOrderBySessionStartTimeDesc(Long barId);

}
