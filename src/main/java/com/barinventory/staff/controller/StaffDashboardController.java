package com.barinventory.staff.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.User;
import com.barinventory.common.controller.BaseBarController;
import com.barinventory.common.service.InventorySessionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffDashboardController extends BaseBarController {

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal User user, HttpSession httpSession, Model model) {
		requireStaffRole(httpSession);

		Long barId = getActiveBarId(httpSession);
		model.addAttribute("username", user.getName());
		model.addAttribute("barId", barId);

		return "staff/staff-dashboard";
	}
}

@Controller
@RequestMapping("/staff/sessions")
@RequiredArgsConstructor
class StaffSessionController extends BaseBarController {

	private final InventorySessionService sessionService;

	@GetMapping("/{sessionId}/wells")
	public String wells(@PathVariable Long sessionId, HttpSession httpSession, Model model) {
		validateSessionAccess(sessionId, httpSession, sessionService);

		InventorySession session = sessionService.getSession(sessionId);
		model.addAttribute("session", session);
		model.addAttribute("wells", sessionService.getWellsByBar(session.getBar().getBarId()));

		return "sessions/wells";
	}

	@PostMapping("/{sessionId}/wells/save")
	@ResponseBody
	public ResponseEntity<?> saveWells(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			HttpSession httpSession) {
		try {
			validateSessionAccess(sessionId, httpSession, sessionService);
			sessionService.saveWellInventoryFromForm(sessionId, formData);
			return ResponseEntity.ok(Map.of("status", "success"));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}