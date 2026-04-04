package com.barinventory.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barinventory.customer.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	List<CartItem> findByCustomerId(Long customerId);

	Optional<CartItem> findByCustomerIdAndBrandSizeId(Long customerId, Long brandSizeId);

	void deleteByCustomerId(Long customerId);
}