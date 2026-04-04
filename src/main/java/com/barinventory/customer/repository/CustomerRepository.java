package com.barinventory.customer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barinventory.customer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByPhone(String phone);
}