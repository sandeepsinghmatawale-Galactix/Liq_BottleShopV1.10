package com.barinventory.customer.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.customer.dto.CustomerRegistrationDTO;
import com.barinventory.customer.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerAuthController {

	private final CustomerService customerService;

	@GetMapping({ "", "/" })
	public String customerRoot(Principal principal) {
		return principal != null ? "redirect:/customer/dashboard" : "redirect:/customer/login";
	}

	@GetMapping("/login")
	public String loginPage(@RequestParam(required = false) String error,
			@RequestParam(required = false) String registered, @RequestParam(required = false) String logout,
			Model model) {

		if ("403".equals(error)) {
			model.addAttribute("error", "Please login as customer to continue.");
		} else if (error != null) {
			model.addAttribute("error", "Invalid email or password");
		}

		if (registered != null) {
			model.addAttribute("success", "Registration successful. Please login.");
		}

		if (logout != null) {
			model.addAttribute("success", "Logged out successfully.");
		}

		return "customer/login";
	}

	@GetMapping("/register")
	public String registerPage(Model model) {
		model.addAttribute("registration", new CustomerRegistrationDTO());
		return "customer/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registration") CustomerRegistrationDTO dto, BindingResult result,
			Model model) {

		if (result.hasErrors()) {
			return "customer/register";
		}

		try {
			customerService.register(dto);
			return "redirect:/customer/login?registered=true";
		} catch (RuntimeException e) {
			model.addAttribute("error", e.getMessage());
			return "customer/register";
		}
	}
}
