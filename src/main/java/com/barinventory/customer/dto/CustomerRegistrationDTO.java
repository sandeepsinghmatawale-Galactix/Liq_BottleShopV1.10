package com.barinventory.customer.dto;

import java.time.LocalDate;

import com.barinventory.customer.entity.Customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegistrationDTO {
	private String name;
	private String email;
	private String phone;
	private String password;
	private LocalDate dateOfBirth;
	private Customer.Gender gender;
	private Double weight;
}