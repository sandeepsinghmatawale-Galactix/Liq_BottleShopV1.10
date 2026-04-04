
package com.barinventory.customer.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.barinventory.customer.entity.CustomerOrder;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);

	@Query("SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.items WHERE o.id = :id")
	CustomerOrder findByIdWithItems(@Param("id") Long id);

	@Query("SELECT SUM(o.totalAmount) FROM CustomerOrder o WHERE o.customer.id = :customerId "
			+ "AND o.orderDate BETWEEN :start AND :end AND o.status = 'COMPLETED'")
	Double getTotalSpent(@Param("customerId") Long customerId, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);
}