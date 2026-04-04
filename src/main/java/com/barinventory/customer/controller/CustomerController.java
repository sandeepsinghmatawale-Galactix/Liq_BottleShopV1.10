package com.barinventory.customer.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.barinventory.brands.entity.Brand;
import com.barinventory.brands.service.BrandService;
import com.barinventory.customer.dto.CartItemDTO;
import com.barinventory.customer.dto.ConsumptionLogDTO;
import com.barinventory.customer.dto.HealthStatsDTO;
import com.barinventory.customer.entity.Customer;
import com.barinventory.customer.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;
	private final BrandService brandService;
	
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal Customer customer, Model model) {
		model.addAttribute("customer", customer);
		return "customer/dashboard";
	}


	@GetMapping({"/explore", "/brands"})
	public String explore(Model model) {
	    List<Brand> brands = brandService.getAllActiveBrandsForCustomer();

	    List<String> categories = brands.stream()
	            .map(Brand::getCategory)
	            .filter(Objects::nonNull)
	            .map(Enum::name)
	            .distinct()
	            .sorted()
	            .toList();

	    model.addAttribute("brands", brands);
	    model.addAttribute("categories", categories);
	    return "customer/explore";
	}


	@GetMapping("/cart")
	public String cart(@AuthenticationPrincipal Customer customer, Model model) {
		List<CartItemDTO> items = customerService.getCart(customer.getId());
		model.addAttribute("cartItems", items);
		return "customer/cart";
	}

	@PostMapping("/cart/add")
	public String addToCart(@AuthenticationPrincipal Customer customer,
			@RequestParam Long brandSizeId,
			@RequestParam Integer quantity) {
		customerService.addToCart(customer.getId(), brandSizeId, quantity);
		return "redirect:/customer/cart";
	}

	@PostMapping("/cart/checkout")
	public String checkout(@AuthenticationPrincipal Customer customer) {
		customerService.checkout(customer.getId());
		return "redirect:/customer/orders";
	}

	@GetMapping("/orders")
	public String orders(@AuthenticationPrincipal Customer customer, Model model) {
		model.addAttribute("orders", customerService.getOrderHistory(customer.getId()));
		return "customer/orders";
	}

	@GetMapping("/health")
	public String health(@AuthenticationPrincipal Customer customer, Model model) {
		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = end.minusDays(30);

		HealthStatsDTO stats = customerService.getHealthStats(customer.getId(), start, end);
		var logs = customerService.getConsumptionHistory(customer.getId(), start, end);

		model.addAttribute("stats", stats);
		model.addAttribute("logs", logs);
		return "customer/health";
	}

	@PostMapping({"/api/consumption/log", "/consumption/log"})
	@ResponseBody
	public String logConsumption(@AuthenticationPrincipal Customer customer,
			@RequestBody ConsumptionLogDTO dto) {
		customerService.logConsumption(customer.getId(), dto);
		return "success";
	}
}
