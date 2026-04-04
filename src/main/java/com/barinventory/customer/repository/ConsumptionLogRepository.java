package com.barinventory.customer.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.barinventory.customer.entity.ConsumptionLog;

public interface ConsumptionLogRepository extends JpaRepository<ConsumptionLog, Long> {

	List<ConsumptionLog> findByCustomerIdOrderByConsumptionTimeDesc(Long customerId);

	List<ConsumptionLog> findByCustomerIdAndConsumptionTimeBetween(Long customerId, LocalDateTime start,
			LocalDateTime end);

	@Query("SELECT SUM(c.unitsConsumed) FROM ConsumptionLog c WHERE c.customer.id = :customerId "
			+ "AND c.consumptionTime BETWEEN :start AND :end")
	Integer getTotalUnitsConsumed(@Param("customerId") Long customerId, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);
}