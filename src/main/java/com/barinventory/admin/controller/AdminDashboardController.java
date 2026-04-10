package com.barinventory.admin.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.barinventory.admin.dto.BarSubscriptionRequest;
import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.common.controller.BaseBarController;
import com.barinventory.common.exception.BarAccessDeniedException;
import com.barinventory.common.service.BarService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController extends BaseBarController {

	private final BarRepository barRepository;
	private final BarService barService;
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal User user, Model model) {

	    if (user.getRole() != Role.ADMIN) {
	        throw new BarAccessDeniedException("Admin access required");
	    }

	    model.addAttribute("bars", barRepository.findAll());
	    model.addAttribute("username", user.getName());

	    return "admin/dashboard";
	}

	 

	 

	private BarSubscriptionRequest createRequest(String endDate, Integer freeLimit) {
		BarSubscriptionRequest req = new BarSubscriptionRequest();
		if (endDate != null && !endDate.isBlank()) {
			req.setSubscriptionStartDate(LocalDate.now());
			req.setSubscriptionEndDate(LocalDate.parse(endDate));
		}
		req.setFreeLoginLimit(freeLimit);
		return req;
	}
}