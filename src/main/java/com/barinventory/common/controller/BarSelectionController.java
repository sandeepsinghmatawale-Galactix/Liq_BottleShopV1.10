package com.barinventory.common.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.repository.BarRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BarSelectionController extends BaseBarController {

	private final BarRepository barRepository;

	@GetMapping("/select-bar")
	public String selectBarPage(@AuthenticationPrincipal User user, Model model) {
		model.addAttribute("bars", user.getBars()); // assuming relation exists
		return "select-bar";
	}

	@PostMapping("/switch-bar")
	public String switchBar(@RequestParam Long barId, @AuthenticationPrincipal User user, HttpSession session) {

		Bar bar = barRepository.findById(barId).orElseThrow(() -> new RuntimeException("Bar not found"));

		// ✅ STORE IN SESSION (MOST IMPORTANT)
		session.setAttribute(ACTIVE_BAR_ID, bar.getBarId());
		session.setAttribute(ACTIVE_BAR_ROLE, user.getRole());

		// ✅ ROLE BASED REDIRECT
		if (user.getRole() == Role.BAR_OWNER) {
			return "redirect:/owner/dashboard";
		} else if (user.getRole() == Role.BAR_STAFF) {
			return "redirect:/staff/dashboard";
		}

		return "redirect:/login";
	}
}