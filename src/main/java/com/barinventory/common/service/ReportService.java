package com.barinventory.common.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.SalesRecord;
import com.barinventory.admin.repository.InventorySessionRepository;
import com.barinventory.admin.repository.SalesRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

	private final SalesRecordRepository salesRepository;
	private final InventorySessionRepository sessionRepository;

	/**
	 * Get total sales for a session
	 */
	public BigDecimal getSessionTotalSales(Long sessionId) {
		BigDecimal result = salesRepository.getTotalRevenueBySession(sessionId);
		return result != null ? result : BigDecimal.ZERO;
	}

	/**
	 * Get sales records for a date range
	 */
	public List<SalesRecord> getSalesByDateRange(Long barId, LocalDateTime startDate, LocalDateTime endDate) {
		return salesRepository.findSalesByBarAndDateRange(barId, startDate, endDate);
	}

	/**
	 * Get daily sales report
	 */
	public Map<String, Object> getDailySalesReport(Long barId, LocalDateTime date) {
		LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
		LocalDateTime endOfDay = startOfDay.plusDays(1);

		List<SalesRecord> sales = getSalesByDateRange(barId, startOfDay, endOfDay);

		BigDecimal totalRevenue = sales.stream().map(s -> safe(s.getTotalRevenue())).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		BigDecimal totalCost = sales.stream().map(s -> safe(s.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalProfit = totalRevenue.subtract(totalCost);

		return Map.of("date", date, "salesRecords", sales, "totalRevenue", totalRevenue, "totalCost", totalCost,
				"totalProfit", totalProfit);
	}

	/**
	 * Get weekly sales report
	 */
	public Map<String, Object> getWeeklySalesReport(Long barId, LocalDateTime weekStart) {
		LocalDateTime weekEnd = weekStart.plusDays(7);
		List<SalesRecord> sales = getSalesByDateRange(barId, weekStart, weekEnd);

		BigDecimal totalRevenue = sales.stream().map(s -> safe(s.getTotalRevenue())).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		return Map.of("weekStart", weekStart, "weekEnd", weekEnd, "salesRecords", sales, "totalRevenue", totalRevenue);
	}

	/**
	 * Get monthly sales report
	 */
	public Map<String, Object> getMonthlySalesReport(Long barId, int year, int month) {
		LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
		LocalDateTime monthEnd = monthStart.plusMonths(1);

		List<SalesRecord> sales = getSalesByDateRange(barId, monthStart, monthEnd);

		BigDecimal totalRevenue = sales.stream().map(s -> safe(s.getTotalRevenue())).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		// ✅ FIXED: BrandSize instead of Product
		Map<String, BigDecimal> productWiseSales = sales.stream()
				.collect(Collectors.groupingBy(this::getProductKey, Collectors.mapping(s -> safe(s.getTotalRevenue()),
						Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

		Map<String, Object> result = new java.util.HashMap<>();
		result.put("year", year);
		result.put("month", month);
		result.put("salesRecords", sales);
		result.put("totalRevenue", totalRevenue);
		result.put("productWiseSales", productWiseSales);

		return result;
	}

	/**
	 * Get audit trail for sessions
	 */
	public List<InventorySession> getAuditTrail(Long barId, LocalDateTime startDate, LocalDateTime endDate) {
		return sessionRepository.findSessionsByBarAndDateRange(barId, startDate, endDate);
	}

	/**
	 * Get product-wise sales summary
	 */
	public Map<String, Object> getProductWiseSummary(Long barId, LocalDateTime startDate, LocalDateTime endDate) {
		List<SalesRecord> sales = getSalesByDateRange(barId, startDate, endDate);

		// ✅ FIXED: BrandSize instead of Product
		Map<String, Map<String, Object>> productSummary = sales.stream()
				.collect(Collectors.groupingBy(this::getProductKey)).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> {
					List<SalesRecord> list = e.getValue();

					BigDecimal totalQty = list.stream().map(s -> safe(s.getQuantitySold())).reduce(BigDecimal.ZERO,
							BigDecimal::add);

					BigDecimal totalRevenue = list.stream().map(s -> safe(s.getTotalRevenue())).reduce(BigDecimal.ZERO,
							BigDecimal::add);

					Map<String, Object> map = new java.util.HashMap<>();
					map.put("totalQuantity", totalQty);
					map.put("totalRevenue", totalRevenue);
					map.put("count", list.size());

					return map;
				}));

		Map<String, Object> result = new java.util.HashMap<>();
		result.put("productSummary", productSummary);
		return result;
	}

	// =========================================================
	// HELPER
	// =========================================================
	private BigDecimal safe(BigDecimal val) {
		return val == null ? BigDecimal.ZERO : val;
	}

	private String getProductKey(SalesRecord s) {
		if (s.getBrandSize() == null)
			return "Unknown";

		String brand = s.getBrandSize().getBrand() != null ? s.getBrandSize().getBrand().getBrandName() : "Unknown";

		String size = s.getBrandSize().getSizeLabel() != null ? s.getBrandSize().getSizeLabel() : "";

		return brand + " " + size;
	}

	public Map<String, Object> getQuarterlyReport(Long barId, int year, int quarter) {

		int startMonth = (quarter - 1) * 3 + 1;

		LocalDateTime start = LocalDateTime.of(year, startMonth, 1, 0, 0);
		LocalDateTime end = start.plusMonths(3);

		List<SalesRecord> sales = getSalesByDateRange(barId, start, end);

		BigDecimal totalRevenue = sales.stream().map(s -> safe(s.getTotalRevenue())).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		return Map.of("year", year, "quarter", quarter, "totalRevenue", totalRevenue, "salesRecords", sales);
	}

	public Map<String, Object> getYearlyReport(Long barId, int year) {

		LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
		LocalDateTime end = start.plusYears(1);

		List<SalesRecord> sales = getSalesByDateRange(barId, start, end);

		BigDecimal totalRevenue = sales.stream().map(s -> safe(s.getTotalRevenue())).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		return Map.of("year", year, "totalRevenue", totalRevenue, "salesRecords", sales);
	}

	public Map<String, Object> getCustomReport(Long barId, LocalDateTime start, LocalDateTime end) {

		List<SalesRecord> sales = getSalesByDateRange(barId, start, end);

		BigDecimal revenue = sales.stream().map(s -> safe(s.getTotalRevenue())).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		BigDecimal cost = sales.stream().map(s -> safe(s.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add);

		return Map.of("startDate", start, "endDate", end, "totalRevenue", revenue, "totalCost", cost, "profit",
				revenue.subtract(cost), "salesRecords", sales);
	}

}