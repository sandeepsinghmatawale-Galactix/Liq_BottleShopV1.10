package com.barinventory.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barinventory.common.service.BarService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/bars")
@RequiredArgsConstructor
public class AdminBarManagementController {

	private final BarService barService;

	@PostMapping("/{barId}/activate")
	public String activateBar(@PathVariable Long barId, RedirectAttributes redirectAttributes) {
		barService.activateBar(barId);
		redirectAttributes.addFlashAttribute("success", "Bar activated successfully");
		return "redirect:/admin/dashboard";
	}

	@PostMapping("/{barId}/hide")
	public String hideBar(@PathVariable Long barId, RedirectAttributes redirectAttributes) {
		barService.hideBar(barId);
		redirectAttributes.addFlashAttribute("success", "Bar hidden successfully");
		return "redirect:/admin/dashboard";
	}

	@PostMapping("/{barId}/unhide")
	public String unhideBar(@PathVariable Long barId, RedirectAttributes redirectAttributes) {
		barService.unhideBar(barId);
		redirectAttributes.addFlashAttribute("success", "Bar unhidden successfully");
		return "redirect:/admin/dashboard";
	}
}