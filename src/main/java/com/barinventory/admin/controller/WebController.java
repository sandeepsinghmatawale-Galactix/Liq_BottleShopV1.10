package com.barinventory.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.BarProductPrice;
import com.barinventory.admin.entity.BarWell;
import com.barinventory.admin.entity.DistributionRecord;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.StockroomInventory;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.WellInventory;
import com.barinventory.admin.enums.SessionStatus;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.admin.repository.InventorySessionRepository;
import com.barinventory.admin.repository.UserRepository;
import com.barinventory.admin.service.BarService;
import com.barinventory.admin.service.BrandSizeProductService;
import com.barinventory.admin.service.InventorySessionService;
import com.barinventory.admin.service.PricingService;
import com.barinventory.admin.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

	// =========================================================
	// SERVICES & REPOSITORIES
	// =========================================================
	private final InventorySessionService sessionService;
	private final BarService barService;
	private final PricingService pricingService;
	private final ReportService reportService;
	private final UserRepository userRepository;
	private final BarRepository barRepository;
    private final BrandSizeProductService  brandSizeProductService;
	private final InventorySessionRepository sessionRepository;

	// =========================================================
	// LOGIN & AUTHENTICATION
	// =========================================================

	@GetMapping("/login")
	public String showLogin() {
		return "login";
	}

	@GetMapping("/")
	public String redirectToDashboard(@AuthenticationPrincipal User user) {
		if (user != null && user.getUsername() != null) {
			return "redirect:/dashboard";
		}
		return "redirect:/login";
	}

	// =========================================================
	// DASHBOARD
	// =========================================================

	@GetMapping("/dashboard")
	public String showDashboard(@AuthenticationPrincipal User user, Model model) {

	    if (user == null) {
	        return "redirect:/login";
	    }

	    model.addAttribute("activePage", "dashboard");
	    model.addAttribute("user", user);
	    model.addAttribute("username", user.getName());
	    model.addAttribute("role", user.getRole());

	    if (user.getRole() == Role.ADMIN) {
	        List<Bar> bars = barRepository.findAll();
	        model.addAttribute("bars", bars);
	        model.addAttribute("totalBars", bars.size());
	        model.addAttribute("totalUsers", userRepository.count());
	        return "dashboard";
	    }

	    Long barId = user.getBarId();
	    if (barId == null) {
	        return "redirect:/login?error=no_bar";
	    }

	    Bar bar = barRepository.findById(barId)
	            .orElseThrow(() -> new RuntimeException("Bar not found: " + barId));

	    List<InventorySession> sessions = sessionService.getSessionsByBar(barId);

	    model.addAttribute("bar", bar);
	    model.addAttribute("barId", barId);
	    model.addAttribute("sessions", sessions);
	    model.addAttribute("activeSessionCount",
	            sessions.stream().filter(s -> s.getStatus() == SessionStatus.IN_PROGRESS).count());

	    if (user.getRole() == Role.BAR_OWNER) {
	        long staffCount = userRepository.countUsersByBarAndRole(barId, Role.BAR_STAFF);
	        model.addAttribute("staffCount", staffCount);
	    }

	    return "dashboard";
	}


	// =========================================================
	// SESSION MANAGEMENT
	// =========================================================

	@GetMapping("/sessions/new/{barId}")
	public String newSession(@PathVariable Long barId, Model model) {

		Bar bar = barRepository.findById(barId).orElseThrow(() -> new RuntimeException("Bar not found: " + barId));

		// Create new session
		InventorySession session = sessionService.createSession(barId);

		model.addAttribute("session", session);
		model.addAttribute("bar", bar);
		model.addAttribute("sessionId", session.getSessionId());

		return "redirect:/sessions/stockroom/" + session.getSessionId();
	}

	@GetMapping("/sessions/{sessionId}")
	public String viewSession(@PathVariable Long sessionId, Model model) {

		InventorySession session = sessionService.getSession(sessionId);

		model.addAttribute("session", session);
		model.addAttribute("bar", session.getBar());
		model.addAttribute("status", session.getStatus());

		// Get counts
		long stockroomCount = sessionService.getStockroomBySession(sessionId).size();
		long wellCount = sessionService.getWellsBySession(sessionId).size();
		long distributionCount = sessionService.getDistributionsBySession(sessionId).size();

		model.addAttribute("stockroomCount", stockroomCount);
		model.addAttribute("wellCount", wellCount);
		model.addAttribute("distributionCount", distributionCount);

		return "sessions/view";
	}

	// =========================================================
	// STOCKROOM PAGE
	// =========================================================
	@GetMapping("/sessions/stockroom/{sessionId}")
	public String stockroom(@PathVariable Long sessionId, Model model) {

	    InventorySession inv = sessionService.getSession(sessionId);

	    model.addAttribute("inv", inv);
	    model.addAttribute("products", brandSizeProductService.getAllActiveProducts());

	    // optional: keep new names too if needed
	    model.addAttribute("session", inv);
	    model.addAttribute("sessionId", sessionId);

	    return "stockroom";
	}


	@PostMapping("/sessions/stockroom/{sessionId}")
	@Transactional
	public String saveStockroom(@PathVariable Long sessionId, @RequestParam Map<String, String> formData, Model model) {

		try {
			// Parse and save form data
			sessionService.saveStockroomFromForm(sessionId, formData);

			log.info("Stockroom saved for session: {}", sessionId);

			// Redirect to distribution step
			return "redirect:/sessions/distribution/" + sessionId;

		} catch (Exception e) {
			log.error("Error saving stockroom for session {}: {}", sessionId, e.getMessage());

			InventorySession session = sessionService.getSession(sessionId);
			Long barId = session.getBar().getBarId();

			model.addAttribute("session", session);
			model.addAttribute("sessionId", sessionId);
			model.addAttribute("barId", barId);
			model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
			model.addAttribute("previousClosing", sessionService.getPreviousClosingForStockroom(barId));
			model.addAttribute("error", e.getMessage());

			return "sessions/stockroom";
		}
	}

	// =========================================================
	// DISTRIBUTION PAGE
	// =========================================================

	@GetMapping("/sessions/distribution/{sessionId}")
	public String distribution(@PathVariable Long sessionId, Model model) {

		InventorySession session = sessionService.getSession(sessionId);
		Bar bar = session.getBar();

		// Get all wells for this bar
		List<BarWell> wells = sessionService.getWellsByBar(bar.getBarId());

		// Get distribution records
		List<DistributionRecord> distributions = sessionService.getDistributionsBySession(sessionId);

		// Get stockroom data (for reference)
		List<StockroomInventory> stockrooms = sessionService.getStockroomBySession(sessionId);

		model.addAttribute("session", session);
		model.addAttribute("bar", bar);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("wells", wells);
		model.addAttribute("distributions", distributions);
		model.addAttribute("stockrooms", stockrooms);

		return "sessions/distribution";
	}

	@PostMapping("/sessions/distribution/{sessionId}/save")
	public String saveDistribution(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			Model model) {

		try {
			// Validate before saving
			StringBuilder errors = new StringBuilder();
			if (!sessionService.validateStockroomToDistribution(sessionId, errors)) {
				throw new RuntimeException("Stockroom validation failed: " + errors.toString());
			}

			// Parse and save distribution allocations
			sessionService.saveDistributionAllocations(sessionId, formData);

			log.info("Distribution allocations saved for session: {}", sessionId);

			// Redirect to wells step
			return "redirect:/sessions/wells/" + sessionId;

		} catch (Exception e) {
			log.error("Error saving distribution for session {}: {}", sessionId, e.getMessage());

			InventorySession session = sessionService.getSession(sessionId);
			Bar bar = session.getBar();

			model.addAttribute("session", session);
			model.addAttribute("bar", bar);
			model.addAttribute("sessionId", sessionId);
			model.addAttribute("wells", sessionService.getWellsByBar(bar.getBarId()));
			model.addAttribute("distributions", sessionService.getDistributionsBySession(sessionId));
			model.addAttribute("error", e.getMessage());

			return "sessions/distribution";
		}
	}

	// =========================================================
	// WELLS PAGE
	// =========================================================

	@GetMapping("/sessions/wells/{sessionId}")
	public String wells(@PathVariable Long sessionId, Model model) {

		InventorySession session = sessionService.getSession(sessionId);
		Bar bar = session.getBar();

		// Get all wells for this bar
		List<BarWell> wells = sessionService.getWellsByBar(bar.getBarId());

		// Get pricing map
		Map<Long, BarProductPrice> prices = pricingService.getPriceMapForBar(bar.getBarId());
		 

		// Get distribution allocations map
		Map<Long, BigDecimal> distributionMap = sessionService.getDistributionMapForSession(sessionId);

		// Get previous closing for wells
		BigDecimal previousClosing = sessionService.getPreviousClosingForWells(bar.getBarId());

		// Get brand sizes
		List<com.barinventory.brands.entity.BrandSize> brandSizes = sessionService.getAllActiveBrandSizes();

		// Get existing well records (if editing)
		List<WellInventory> existingWells = sessionService.getWellsBySession(sessionId);

		model.addAttribute("session", session);
		model.addAttribute("bar", bar);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("wells", wells);
		model.addAttribute("prices", prices);
		model.addAttribute("distributionMap", distributionMap);
		model.addAttribute("previousClosing", previousClosing);
		model.addAttribute("brandSizes", brandSizes);
		model.addAttribute("wellRecords", existingWells);

		return "sessions/wells";
	}

	@PostMapping("/sessions/wells/{sessionId}/save")
	@ResponseBody
	public ResponseEntity<?> saveWells(@PathVariable Long sessionId, @RequestParam Map<String, String> formData) {

		try {
			// Parse and save well inventory
			sessionService.saveWellInventoryFromForm(sessionId, formData);

			log.info("Well inventory saved for session: {}", sessionId);

			return ResponseEntity.ok(Map.of("status", "success", "sessionId", sessionId));

		} catch (Exception e) {
			log.error("Error saving wells for session {}: {}", sessionId, e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	// =========================================================
	// VERIFICATION & COMPLETION
	// =========================================================

	@GetMapping("/sessions/verify/{sessionId}")
	public String verifySession(@PathVariable Long sessionId, Model model) {

		InventorySession session = sessionService.getSession(sessionId);

		// Run all validations
		StringBuilder errors = new StringBuilder();

		boolean valid1 = sessionService.validateStockroomToDistribution(sessionId, errors);
		boolean valid2 = sessionService.validateDistributionToWells(sessionId, errors);
		boolean valid3 = sessionService.validateNoUnallocatedStock(sessionId, errors);

		boolean allValid = valid1 && valid2 && valid3;

		model.addAttribute("session", session);
		model.addAttribute("isValid", allValid);
		model.addAttribute("errors", errors.toString());

		// Get summary data
		List<StockroomInventory> stockrooms = sessionService.getStockroomBySession(sessionId);
		List<DistributionRecord> distributions = sessionService.getDistributionsBySession(sessionId);
		List<WellInventory> wells = sessionService.getWellsBySession(sessionId);

		model.addAttribute("stockroomCount", stockrooms.size());
		model.addAttribute("distributionCount", distributions.size());
		model.addAttribute("wellCount", wells.size());

		return "sessions/verify";
	}

	@PostMapping("/sessions/{sessionId}/complete")
	public String completeSession(@PathVariable Long sessionId, Model model) {

		try {
			// Final validation
			StringBuilder errors = new StringBuilder();

			if (!sessionService.validateStockroomToDistribution(sessionId, errors)
					|| !sessionService.validateDistributionToWells(sessionId, errors)
					|| !sessionService.validateNoUnallocatedStock(sessionId, errors)) {

				throw new RuntimeException("Validation failed: " + errors.toString());
			}

			// Update session status to COMPLETED
			sessionService.updateSessionStatus(sessionId, SessionStatus.COMPLETED);

			log.info("Session {} completed successfully", sessionId);

			return "redirect:/dashboard?sessionCompleted=" + sessionId;

		} catch (Exception e) {
			log.error("Error completing session {}: {}", sessionId, e.getMessage());

			InventorySession session = sessionService.getSession(sessionId);

			model.addAttribute("session", session);
			model.addAttribute("error", e.getMessage());

			return "sessions/verify";
		}
	}

	// =========================================================
	// REPORTS
	// =========================================================

	@GetMapping("/reports/{barId}/daily")
	public String report(@PathVariable Long barId, @RequestParam(required = false) String date, Model model) {

		Bar bar = barService.getBarById(barId);

		LocalDateTime reportDate = (date != null && !date.isEmpty()) ? LocalDateTime.parse(date) : LocalDateTime.now();

		Map<String, Object> report = reportService.getDailySalesReport(barId, reportDate);

		model.addAttribute("bar", bar);
		model.addAttribute("reportDate", reportDate);
		model.addAttribute("report", report);

		return "reports/daily";
	}

	@GetMapping("/reports/{barId}/sessions")
	public String sessionHistory(@PathVariable Long barId, Model model) {

		Bar bar = barRepository.findById(barId).orElseThrow(() -> new RuntimeException("Bar not found: " + barId));

		List<InventorySession> sessions = sessionRepository.findByBarBarIdOrderBySessionStartTimeDesc(barId);

		model.addAttribute("bar", bar);
		model.addAttribute("sessions", sessions);

		return "reports/sessions";
	}

	// =========================================================
	// API ENDPOINTS (for AJAX calls)
	// =========================================================

	@GetMapping("/api/wells/{barId}")
	@ResponseBody
	public ResponseEntity<?> getWellsByBar(@PathVariable Long barId) {
		try {
			List<BarWell> wells = sessionService.getWellsByBar(barId);
			return ResponseEntity.ok(wells);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/api/brandSizes")
	@ResponseBody
	public ResponseEntity<?> getBrandSizes() {
		try {
			List<com.barinventory.brands.entity.BrandSize> sizes = sessionService.getAllActiveBrandSizes();
			return ResponseEntity.ok(sizes);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/api/session/{sessionId}/status")
	@ResponseBody
	public ResponseEntity<?> getSessionStatus(@PathVariable Long sessionId) {
		try {
			InventorySession session = sessionService.getSession(sessionId);
			return ResponseEntity.ok(Map.of("sessionId", session.getSessionId(), "status", session.getStatus(), "barId",
					session.getBar().getBarId()));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	// =========================================================
	// ERROR HANDLING
	// =========================================================

	@ExceptionHandler(Exception.class)
	public String handleException(Exception e, Model model) {
		log.error("Unexpected error: ", e);
		model.addAttribute("error", e.getMessage());
		return "error";
	}
}