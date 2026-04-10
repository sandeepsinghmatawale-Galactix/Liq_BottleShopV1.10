package com.barinventory.owner.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
			HttpSession session, Model model) {

		validateBarAccess(barId, session);

		LocalDateTime reportDate = (date != null && !date.isEmpty()) ? LocalDateTime.parse(date) : LocalDateTime.now();

		Map<String, Object> report = reportService.getDailySalesReport(barId, reportDate);

		model.addAttribute("report", report);
		model.addAttribute("reportDate", reportDate);

		return "owner/reports/daily";
	}
}