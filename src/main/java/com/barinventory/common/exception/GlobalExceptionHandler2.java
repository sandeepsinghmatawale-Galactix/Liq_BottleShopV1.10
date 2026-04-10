package com.barinventory.common.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler2 {

	// ✅ Redirect if bar not selected
	@ExceptionHandler(BarNotSelectedException.class)
	public String handleBarNotSelected() {
		return "redirect:/select-bar";
	}

	// ❌ Access denied
	@ExceptionHandler(BarAccessDeniedException.class)
	public String handleAccessDenied(Model model) {
		model.addAttribute("error", "Access Denied");
		return "error/403";
	}

	@ExceptionHandler(Exception.class)
	public String handleGeneral(Exception ex, Model model) {

		ex.printStackTrace(); // 🔥 IMPORTANT

		model.addAttribute("error", ex.getMessage());
		return "error/500";
	}
}