package com.barinventory.owner.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.User;
import com.barinventory.common.controller.BaseBarController;
import com.barinventory.common.service.InventorySessionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/owner/sessions")
@RequiredArgsConstructor
@Slf4j
public class OwnerSessionController extends BaseBarController {

	private final InventorySessionService sessionService;

	@GetMapping("/new")
	public String newSession(@AuthenticationPrincipal User user, HttpSession httpSession, Model model) {
		requireOwnerRole(httpSession);

		Long barId = getActiveBarId(httpSession);
		InventorySession session = sessionService.createSession(barId);

		return "redirect:/owner/sessions/" + session.getSessionId() + "/stockroom";
	}

	@GetMapping("/{sessionId}/stockroom")
	public String stockroom(@PathVariable Long sessionId, HttpSession httpSession, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			validateSessionAccess(sessionId, httpSession, sessionService);

			InventorySession session = sessionService.getSession(sessionId);
			model.addAttribute("session", session);
			model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());

			return "owner/sessions/stockroom";
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/owner/dashboard";
		}
	}

	@PostMapping("/{sessionId}/stockroom")
	public String saveStockroom(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			HttpSession httpSession, Model model, RedirectAttributes redirectAttributes) {
		try {
			validateSessionAccess(sessionId, httpSession, sessionService);
			sessionService.saveStockroomFromForm(sessionId, formData);
			log.info("Stockroom saved for session: {}", sessionId);
			return "redirect:/owner/sessions/" + sessionId + "/distribution";
		} catch (Exception e) {
			log.error("Error saving stockroom: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/owner/sessions/" + sessionId + "/stockroom";
		}
	}

	@GetMapping("/{sessionId}/distribution")
	public String distribution(@PathVariable Long sessionId, HttpSession httpSession, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			validateSessionAccess(sessionId, httpSession, sessionService);

			InventorySession session = sessionService.getSession(sessionId);
			model.addAttribute("session", session);
			model.addAttribute("wells", sessionService.getWellsByBar(session.getBar().getBarId()));
			model.addAttribute("distributions", sessionService.getDistributionsBySession(sessionId));

			return "owner/sessions/distribution";
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/owner/dashboard";
		}
	}

	@PostMapping("/{sessionId}/distribution/save")
	public String saveDistribution(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			HttpSession httpSession, RedirectAttributes redirectAttributes) {
		try {
			validateSessionAccess(sessionId, httpSession, sessionService);
			sessionService.saveDistributionAllocations(sessionId, formData);
			return "redirect:/owner/sessions/" + sessionId + "/wells";
		} catch (Exception e) {
			log.error("Error saving distribution: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/owner/sessions/" + sessionId + "/distribution";
		}
	}
}