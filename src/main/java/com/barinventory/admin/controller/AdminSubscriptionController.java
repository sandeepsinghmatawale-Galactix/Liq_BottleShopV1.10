package com.barinventory.admin.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barinventory.admin.dto.BarSubscriptionRequest;
import com.barinventory.common.service.BarService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/bars")
@RequiredArgsConstructor
public class AdminSubscriptionController {

	private final BarService barService;

	@PostMapping("/{barId}/subscription/update")
	public String updateSubscription(@PathVariable Long barId,
			@RequestParam(required = false) String subscriptionEndDate,
			@RequestParam(defaultValue = "0") Integer freeLoginLimit, RedirectAttributes redirectAttributes) {
		try {
			BarSubscriptionRequest request = new BarSubscriptionRequest();
			if (subscriptionEndDate != null && !subscriptionEndDate.isBlank()) {
				request.setSubscriptionStartDate(LocalDate.now());
				request.setSubscriptionEndDate(LocalDate.parse(subscriptionEndDate));
			}
			request.setFreeLoginLimit(freeLoginLimit);

			barService.updateSubscription(barId, request);
			redirectAttributes.addFlashAttribute("success", "Subscription updated");
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/dashboard";
	}

	@PostMapping("/{barId}/subscription/block")
	public String blockBar(@PathVariable Long barId, @RequestParam(required = false) String notes,
			RedirectAttributes redirectAttributes) {
		barService.blockBar(barId, notes);
		redirectAttributes.addFlashAttribute("success", "Bar blocked");
		return "redirect:/admin/dashboard";
	}
}
