package com.barinventory.common.service;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.repository.UserRepository;
import com.barinventory.customer.entity.Customer;
import com.barinventory.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;
	private final CustomerRepository customerRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		log.info("Attempting to load user by email: {}", email);

		// Try admin/staff first

		User user = userRepository.findByEmailWithBars(email).orElse(null);
		if (user != null) {
			if (!user.getActive()) {
				log.warn("Disabled admin/staff account login attempt: {}", email);
				throw new UsernameNotFoundException("Account is disabled");
			}
			log.info("User loaded: email={}, role={}, active={}", user.getEmail(), user.getRole(), user.getActive());
			return user;
		}

		// Fallback to customer
		Customer customer = customerRepository.findByEmail(email).orElse(null);
		if (customer != null) {
			if (!customer.getActive()) {
				log.warn("Disabled customer account login attempt: {}", email);
				throw new UsernameNotFoundException("Account is disabled");
			}
			log.info("Customer loaded: email={}, active={}", customer.getEmail(), customer.getActive());
			return customer;
		}

		// Neither found
		log.error("User not found with email: {}", email);
		throw new UsernameNotFoundException("User not found: " + email);
	}
}