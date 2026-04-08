package com.barinventory.owner.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.admin.dto.ReportFilterDTO;
import com.barinventory.common.controller.BaseBarController;
import com.barinventory.common.service.ReportService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/owner/reports")
@RequiredArgsConstructor
public class OwnerReportController extends BaseBarController {

	private final ReportService reportService;

	@GetMapping("/{barId}/daily")
	public String dailyReport(@PathVariable Long barId, @RequestParam(required = false) String date,
			HttpSession httpSession, Model model) {
		validateBarAccess(barId, httpSession);

		LocalDateTime reportDate = (date != null && !date.isEmpty()) ? LocalDateTime.parse(date) : LocalDateTime.now();

		Map<String, Object> report = reportService.getDailySalesReport(barId, reportDate);

		model.addAttribute("report", report);
		model.addAttribute("reportDate", reportDate);

		return "owner/reports/daily";
	}

	@PostMapping("/{barId}/filter")
	public ResponseEntity<?> getReportData(@PathVariable Long barId, @RequestBody ReportFilterDTO filter,
			HttpSession session) {
		try {
			validateBarAccess(barId, session);

			Map<String, Object> result = switch (filter.getType()) {
			case "DAILY" -> reportService.getDailySalesReport(barId, filter.getDate().atStartOfDay());
			case "WEEKLY" -> reportService.getWeeklySalesReport(barId, filter.getDate().atStartOfDay());
			case "MONTHLY" -> reportService.getMonthlySalesReport(barId, filter.getYear(), filter.getMonth());
			default -> throw new IllegalArgumentException("Invalid report type");
			};

			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}