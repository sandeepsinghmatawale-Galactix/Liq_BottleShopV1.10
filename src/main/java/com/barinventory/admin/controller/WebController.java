package com.barinventory.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barinventory.admin.dto.BarSubscriptionRequest;
import com.barinventory.admin.dto.ReportFilterDTO;
import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.BarProductPrice;
import com.barinventory.admin.entity.BarWell;
import com.barinventory.admin.entity.DistributionRecord;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.StockroomInventory;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.UserBarAccess;
import com.barinventory.admin.entity.WellInventory;
import com.barinventory.admin.enums.SessionStatus;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.admin.repository.InventorySessionRepository;
import com.barinventory.admin.repository.UserBarAccessRepository;
import com.barinventory.admin.repository.UserRepository;
import com.barinventory.admin.service.BarService;
import com.barinventory.admin.service.BrandSizeProductService;
import com.barinventory.admin.service.InventorySessionService;
import com.barinventory.admin.service.PricingService;
import com.barinventory.admin.service.ReportService;
import com.barinventory.brands.entity.BrandSize;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

	private static final String ACTIVE_BAR_SESSION_KEY = "activeBarId";
	private static final String ACTIVE_BAR_ACCESS_MODE_KEY = "activeBarAccessMode";
	private static final String FREE_ACCESS_MODE = "FREE";
	private static final String PAID_ACCESS_MODE = "PAID";

	// =========================================================
	// SERVICES & REPOSITORIES
	// =========================================================
	private final InventorySessionService sessionService;
	private final BarService barService;
	private final PricingService pricingService;
	private final ReportService reportService;
	private final UserRepository userRepository;
	private final BarRepository barRepository;
	private final BrandSizeProductService brandSizeProductService;
	private final InventorySessionRepository sessionRepository;
	private final UserBarAccessRepository userBarAccessRepository;
	
	private static final String ACTIVE_BAR_ID = "activeBarId";
    private static final String ACTIVE_BAR_ROLE = "activeBarRole";

	// =========================================================
	// LOGIN & AUTHENTICATION
	// =========================================================

	 @GetMapping("/login")
	    public String showLogin() {
	        return "login";
	    }

	    // =========================================================
	    // ROOT ENTRY
	    // =========================================================

	    @GetMapping("/")
	    public String root(@AuthenticationPrincipal User user, HttpSession session) {

	        if (user == null) return "redirect:/login";

	        if (user.getRole() == Role.ADMIN) {
	            return "redirect:/admin/dashboard";
	        }

	        List<Bar> bars = barService.getActiveBarsForUser(user);

	        if (bars.size() == 1) {
	            return activateBarAndRedirect(user, bars.get(0).getBarId(), session);
	        }

	        return "redirect:/select-bar";
	    }

	    // =========================================================
	    // SELECT BAR
	    // =========================================================

	    @GetMapping("/select-bar")
	    public String selectBarPage(@AuthenticationPrincipal User user,
	                               HttpSession session,
	                               Model model) {

	        // 🔒 सुरक्षा
	        if (user == null) return "redirect:/login";

	        // 🔒 ADMIN should never see this page
	        if (user.getRole() == Role.ADMIN) {
	            return "redirect:/admin/dashboard";
	        }

	        // 📦 Get bars
	        List<Bar> bars = barService.getBarsForSelection(user);

	        List<Bar> activeBars = bars.stream()
	                .filter(bar -> Boolean.TRUE.equals(bar.getActive()))
	                .collect(Collectors.toList());

	        // ⚡ AUTO SELECT if only 1 active bar
	        if (activeBars.size() == 1) {
	            return activateBarAndRedirect(user, activeBars.get(0).getBarId(), session);
	        }

	        // ✅ ADD THIS (FIX FOR YOUR ERROR)
	        model.addAttribute("currentUser", user);

	        // 📦 Send bars to UI
	        model.addAttribute("bars", bars);

	        return "select-bar";
	    }
	    @PostMapping("/select-bar")
	    public String selectBar(@AuthenticationPrincipal User user,
	                            @RequestParam Long barId,
	                            HttpSession session) {

	        return activateBarAndRedirect(user, barId, session);
	    }

	    // =========================================================
	    // ACTIVATE BAR (COMMON LOGIC)
	    // =========================================================

	    private String activateBarAndRedirect(User user, Long barId, HttpSession session) {

	        UserBarAccess access = userBarAccessRepository
	                .findByUser_IdAndBar_BarIdAndActiveTrueAndBar_ActiveTrue(user.getId(), barId)
	                .orElseThrow(() -> new AccessDeniedException("No access"));

	        session.setAttribute(ACTIVE_BAR_ID, barId);
	        session.setAttribute(ACTIVE_BAR_ROLE, access.getBarRole());

	        return redirectToRoleDashboard(user, access.getBarRole());
	    }

	    // =========================================================
	    // ROLE ROUTER (CRITICAL)
	    // =========================================================

	    @GetMapping("/dashboard")
	    public String routeDashboard(@AuthenticationPrincipal User user,
	                                 HttpSession session) {

	        if (user == null) return "redirect:/login";

	        if (user.getRole() == Role.ADMIN) {
	            return "redirect:/admin/dashboard";
	        }

	        Role barRole = (Role) session.getAttribute(ACTIVE_BAR_ROLE);

	        if (barRole == null) {
	            return "redirect:/select-bar";
	        }

	        return redirectToRoleDashboard(user, barRole);
	    }

	    private String redirectToRoleDashboard(User user, Role barRole) {

	        if (user.getRole() == Role.ADMIN) {
	            return "redirect:/admin/dashboard";
	        }

	        if (barRole == Role.BAR_OWNER) {
	            return "redirect:/owner/dashboard";
	        }

	        return "redirect:/staff/dashboard";
	    }

	    // =========================================================
	    // ADMIN DASHBOARD
	    // =========================================================

	    @GetMapping("/admin/dashboard")
	    public String adminDashboard(@AuthenticationPrincipal User user, Model model) {

	        if (user.getRole() != Role.ADMIN) {
	            throw new AccessDeniedException("Unauthorized");
	        }

	        model.addAttribute("bars", barRepository.findAll());
	        model.addAttribute("username", user.getName());

	        return "admin-dashboard";
	    }

	    // =========================================================
	    // OWNER DASHBOARD
	    // =========================================================

	    @GetMapping("/owner/dashboard")
	    public String ownerDashboard(@AuthenticationPrincipal User user,
	                                 HttpSession session,
	                                 Model model) {

	        Role role = (Role) session.getAttribute(ACTIVE_BAR_ROLE);

	        if (role != Role.BAR_OWNER) {
	            throw new AccessDeniedException("Unauthorized");
	        }

	        Long barId = (Long) session.getAttribute(ACTIVE_BAR_ID);

	        if (barId == null) return "redirect:/select-bar";

	        Bar bar = barRepository.findById(barId).orElseThrow();

	        model.addAttribute("username", user.getName());
	        model.addAttribute("bar", bar);

	        return "owner-dashboard";
	    }

	    // =========================================================
	    // STAFF DASHBOARD
	    // =========================================================

	    @GetMapping("/staff/dashboard")
	    public String staffDashboard(@AuthenticationPrincipal User user,
	                                 HttpSession session,
	                                 Model model) {

	        Role role = (Role) session.getAttribute(ACTIVE_BAR_ROLE);

	        if (role != Role.BAR_STAFF) {
	            throw new AccessDeniedException("Unauthorized");
	        }

	        Long barId = (Long) session.getAttribute(ACTIVE_BAR_ID);

	        if (barId == null) return "redirect:/select-bar";

	        Bar bar = barRepository.findById(barId).orElseThrow();

	        model.addAttribute("username", user.getName());
	        model.addAttribute("bar", bar);

	        return "staff-dashboard";
	    }

	    // =========================================================
	    // SWITCH BAR
	    // =========================================================

	    @GetMapping("/switch-bar/{barId}")
	    public String switchBar(@PathVariable Long barId,
	                            @AuthenticationPrincipal User user,
	                            HttpSession session) {

	        return activateBarAndRedirect(user, barId, session);
	    }

	// =========================================================
	// SESSION MANAGEMENT
	// =========================================================

	@GetMapping("/sessions/new/{barId}")
	public String newSession(@PathVariable Long barId, @AuthenticationPrincipal User user, HttpSession httpSession,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			requireSelectedBar(user, barId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

		Bar bar = barRepository.findById(barId).orElseThrow(() -> new RuntimeException("Bar not found: " + barId));

		// Create new session
		InventorySession session = sessionService.createSession(barId);

		model.addAttribute("session", session);
		model.addAttribute("bar", bar);
		model.addAttribute("sessionId", session.getSessionId());

		return "redirect:/sessions/stockroom/" + session.getSessionId();
	}

	@GetMapping("/sessions/{sessionId}")
	public String viewSession(@PathVariable Long sessionId, @AuthenticationPrincipal User user, HttpSession httpSession,
			Model model, RedirectAttributes redirectAttributes) {

		InventorySession session;
		try {
			session = requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

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
	public String stockroom(@PathVariable Long sessionId, @AuthenticationPrincipal User user, HttpSession httpSession,
			Model model, RedirectAttributes redirectAttributes) {

		InventorySession inv;
		try {
			inv = requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

		model.addAttribute("inv", inv);
		model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
		model.addAttribute("session", inv);
		model.addAttribute("sessionId", sessionId);

		return "stockroom";
	}

	@PostMapping("/sessions/stockroom/{sessionId}")
	@Transactional
	public String saveStockroom(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			@AuthenticationPrincipal User user, HttpSession httpSession, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

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
	public String distribution(@PathVariable Long sessionId, @AuthenticationPrincipal User user,
			HttpSession httpSession, Model model, RedirectAttributes redirectAttributes) {

		InventorySession session;
		try {
			session = requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}
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
			@AuthenticationPrincipal User user, HttpSession httpSession, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

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
	public String wells(@PathVariable Long sessionId, @AuthenticationPrincipal User user, HttpSession httpSession,
			Model model, RedirectAttributes redirectAttributes) {

		InventorySession session;
		try {
			session = requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}
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
	public ResponseEntity<?> saveWells(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			@AuthenticationPrincipal User user, HttpSession httpSession) {

		try {
			requireAccessibleSession(user, sessionId, httpSession);

			// Parse and save well inventory
			sessionService.saveWellInventoryFromForm(sessionId, formData);

			log.info("Well inventory saved for session: {}", sessionId);

			return ResponseEntity.ok(Map.of("status", "success", "sessionId", sessionId));

		} catch (AccessDeniedException e) {
			return ResponseEntity.status(403).body(Map.of("status", "error", "message", e.getMessage()));
		} catch (Exception e) {
			log.error("Error saving wells for session {}: {}", sessionId, e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	// =========================================================
	// VERIFICATION & COMPLETION
	// =========================================================

	@GetMapping("/sessions/verify/{sessionId}")
	public String verifySession(@PathVariable Long sessionId, @AuthenticationPrincipal User user,
			HttpSession httpSession, Model model, RedirectAttributes redirectAttributes) {

		InventorySession session;
		try {
			session = requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

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
	public String completeSession(@PathVariable Long sessionId, @AuthenticationPrincipal User user,
			HttpSession httpSession, Model model, RedirectAttributes redirectAttributes) {

		try {
			requireAccessibleSession(user, sessionId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

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

	// ================= ADMIN BAR SETUP (OPENING STOCK) =================

	@GetMapping("/admin/bars/{barId}/setup")
	public String showSetupLanding(@PathVariable Long barId, Model model) {
		model.addAttribute("bar", barService.getBarById(barId));
		model.addAttribute("setupSession", sessionService.getSetupSession(barId).orElse(null));
		return "admin/setup-landing";
	}

	@PostMapping("/admin/bars/{barId}/setup/start")
	public String startSetup(@PathVariable Long barId) {
		InventorySession session = sessionService.createSetupSession(barId);
		return "redirect:/admin/setup/" + session.getSessionId() + "/stockroom";
	}

	@PostMapping("/admin/bars/{barId}/activate")
	public String activateConfiguredBar(@PathVariable Long barId, RedirectAttributes redirectAttributes) {
		barService.activateBar(barId);
		redirectAttributes.addFlashAttribute("barSuccess", "Bar activated successfully.");
		return "redirect:/dashboard";
	}

	@PostMapping("/admin/bars/{barId}/subscription/update")
	public String updateBarSubscription(@PathVariable Long barId,
			@RequestParam(required = false) String subscriptionEndDate,
			@RequestParam(defaultValue = "0") Integer freeLoginLimit,
			@RequestParam(required = false, defaultValue = "false") boolean resetFreeLogins,
			@RequestParam(required = false) String subscriptionNotes, RedirectAttributes redirectAttributes) {
		try {
			BarSubscriptionRequest request = new BarSubscriptionRequest();
			if (subscriptionEndDate != null && !subscriptionEndDate.isBlank()) {
				request.setSubscriptionStartDate(LocalDate.now());
				request.setSubscriptionEndDate(LocalDate.parse(subscriptionEndDate));
			}
			request.setFreeLoginLimit(freeLoginLimit);
			request.setResetFreeLogins(resetFreeLogins);
			request.setSubscriptionNotes(subscriptionNotes);

			if (request.getSubscriptionEndDate() != null) {
				barService.renewSubscription(barId, request);
			} else {
				barService.updateSubscription(barId, request);
			}
			redirectAttributes.addFlashAttribute("barSuccess", "Bar subscription updated successfully.");
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("barError", ex.getMessage());
		}
		return "redirect:/dashboard";
	}

	@PostMapping("/admin/bars/{barId}/subscription/block")
	public String blockBar(@PathVariable Long barId, @RequestParam(required = false) String subscriptionNotes,
			RedirectAttributes redirectAttributes) {
		barService.blockBar(barId, subscriptionNotes);
		redirectAttributes.addFlashAttribute("barSuccess", "Bar blocked successfully.");
		return "redirect:/dashboard";
	}

	@PostMapping("/admin/bars/{barId}/subscription/unblock")
	public String unblockBar(@PathVariable Long barId, RedirectAttributes redirectAttributes) {
		barService.unblockBar(barId);
		redirectAttributes.addFlashAttribute("barSuccess", "Bar unblocked successfully.");
		return "redirect:/dashboard";
	}

	@PostMapping("/admin/bars/{barId}/hide")
	public String hideBar(@PathVariable Long barId, RedirectAttributes redirectAttributes) {
		barService.hideBar(barId);
		redirectAttributes.addFlashAttribute("barSuccess", "Bar hidden successfully.");
		return "redirect:/dashboard";
	}

	@PostMapping("/admin/bars/{barId}/unhide")
	public String unhideBar(@PathVariable Long barId, RedirectAttributes redirectAttributes) {
		barService.unhideBar(barId);
		redirectAttributes.addFlashAttribute("barSuccess", "Bar unhidden successfully.");
		return "redirect:/dashboard";
	}

	@GetMapping("/admin/setup/{sessionId}/stockroom")
	public String showSetupStockroom(@PathVariable Long sessionId, Model model) {
		InventorySession session = sessionService.getSession(sessionId);
		model.addAttribute("session", session);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
		model.addAttribute("existingStock", sessionService.getSetupStockroomData(sessionId));
		return "admin/setup-stockroom";
	}

	@PostMapping("/admin/setup/{sessionId}/stockroom")
	public String saveSetupStockroom(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			Model model) {
		try {
			sessionService.saveSetupStockroom(sessionId, formData);
			return "redirect:/admin/setup/" + sessionId + "/wells";
		} catch (Exception e) {
			InventorySession session = sessionService.getSession(sessionId);
			model.addAttribute("session", session);
			model.addAttribute("sessionId", sessionId);
			model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
			model.addAttribute("existingStock", sessionService.getSetupStockroomData(sessionId));
			model.addAttribute("error", e.getMessage());
			return "admin/setup-stockroom";
		}
	}

	@GetMapping("/admin/setup/{sessionId}/wells")
	public String showSetupWells(@PathVariable Long sessionId, Model model) {
		InventorySession session = sessionService.getSession(sessionId);
		model.addAttribute("session", session);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
		model.addAttribute("wells", sessionService.getWellsByBar(session.getBar().getBarId()));
		model.addAttribute("existingStock", sessionService.getSetupWellsData(sessionId));
		return "admin/setup-wells";
	}

	@PostMapping("/admin/setup/{sessionId}/wells")
	public String saveSetupWells(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			Model model) {
		try {
			sessionService.saveSetupWells(sessionId, formData);
			return "redirect:/admin/setup/" + sessionId + "/confirm";
		} catch (Exception e) {
			InventorySession session = sessionService.getSession(sessionId);
			model.addAttribute("session", session);
			model.addAttribute("sessionId", sessionId);
			model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
			model.addAttribute("wells", sessionService.getWellsByBar(session.getBar().getBarId()));
			model.addAttribute("existingStock", sessionService.getSetupWellsData(sessionId));
			model.addAttribute("error", e.getMessage());
			return "admin/setup-wells";
		}
	}

	@GetMapping("/admin/setup/{sessionId}/confirm")
	public String showSetupConfirm(@PathVariable Long sessionId, Model model) {
		InventorySession session = sessionService.getSession(sessionId);
		model.addAttribute("session", session);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("stockroomData", sessionService.getSetupStockroomData(sessionId));
		model.addAttribute("wellsData", sessionService.getSetupWellsData(sessionId));
		model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
		model.addAttribute("wells", sessionService.getWellsByBar(session.getBar().getBarId()));
		return "admin/setup-confirm";
	}

	@PostMapping("/admin/setup/{sessionId}/finalize")
	public String finalizeSetup(@PathVariable Long sessionId) {
		sessionService.finalizeSetupSession(sessionId);
		return "redirect:/dashboard?setupComplete=true";
	}

	private void populateSetupModel(InventorySession session, Model model) {
		List<BrandSize> brandSizes = sessionService.getAllActiveBrandSizes();
		List<BarWell> wells = sessionService.getWellsByBar(session.getBar().getBarId());

		model.addAttribute("session", session);
		model.addAttribute("sessionId", session.getSessionId());
		model.addAttribute("bar", session.getBar());

		model.addAttribute("brandSizes", brandSizes);
		model.addAttribute("wells", wells);
		model.addAttribute("wellNames", wells.stream().map(BarWell::getWellName).collect(Collectors.toList()));
	}

	// =========================================================
	// REPORTS
	// =========================================================

	@GetMapping("/reports/{barId}/daily")
	public String report(@PathVariable Long barId, @RequestParam(required = false) String date,
			@AuthenticationPrincipal User user, HttpSession httpSession, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			requireSelectedBar(user, barId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

		Bar bar = barService.getBarById(barId);

		LocalDateTime reportDate = (date != null && !date.isEmpty()) ? LocalDateTime.parse(date) : LocalDateTime.now();

		Map<String, Object> report = reportService.getDailySalesReport(barId, reportDate);

		model.addAttribute("bar", bar);
		model.addAttribute("reportDate", reportDate);
		model.addAttribute("report", report);

		return "reportindex";
	}

	@GetMapping("/reports/{barId}/sessions")
	public String sessionHistory(@PathVariable Long barId, @AuthenticationPrincipal User user, HttpSession httpSession,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			requireSelectedBar(user, barId, httpSession);
		} catch (AccessDeniedException ex) {
			return redirectToSelectBar(redirectAttributes, httpSession, ex.getMessage());
		}

		Bar bar = barRepository.findById(barId).orElseThrow(() -> new RuntimeException("Bar not found: " + barId));

		List<InventorySession> sessions = sessionRepository.findByBarBarIdOrderBySessionStartTimeDesc(barId);

		model.addAttribute("bar", bar);
		model.addAttribute("sessions", sessions);

		return "reports/sessions";
	}

	@GetMapping("/reports/{barId}")
	public String reportsPage(@PathVariable Long barId, @AuthenticationPrincipal User user, HttpSession session,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			requireSelectedBar(user, barId, session);
		} catch (Exception e) {
			return redirectToSelectBar(redirectAttributes, session, e.getMessage());
		}

		model.addAttribute("barId", barId);
		return "reports/reportindex";
	}

	@PostMapping("/reports/{barId}/filter")
	@ResponseBody
	public ResponseEntity<?> getReportData(@PathVariable Long barId, @RequestBody ReportFilterDTO filter,
			@AuthenticationPrincipal User user, HttpSession session) {

		try {
			requireSelectedBar(user, barId, session);

			Map<String, Object> result;

			switch (filter.getType()) {

			case "DAILY":
				result = reportService.getDailySalesReport(barId, filter.getDate().atStartOfDay());
				break;

			case "WEEKLY":
				result = reportService.getWeeklySalesReport(barId, filter.getDate().atStartOfDay());
				break;

			case "MONTHLY":
				result = reportService.getMonthlySalesReport(barId, filter.getYear(), filter.getMonth());
				break;

			case "QUARTERLY":
				result = reportService.getQuarterlyReport(barId, filter.getYear(), filter.getQuarter());
				break;

			case "YEARLY":
				result = reportService.getYearlyReport(barId, filter.getYear());
				break;

			case "CUSTOM":
				result = reportService.getCustomReport(barId, filter.getStartDate().atStartOfDay(),
						filter.getEndDate().atTime(23, 59));
				break;

			default:
				return ResponseEntity.badRequest().body("Invalid type");
			}

			return ResponseEntity.ok(result);

		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// =========================================================
	// API ENDPOINTS (for AJAX calls)
	// =========================================================

	@GetMapping("/api/wells/{barId}")
	@ResponseBody
	public ResponseEntity<?> getWellsByBar(@PathVariable Long barId, @AuthenticationPrincipal User user,
			HttpSession httpSession) {
		try {
			requireSelectedBar(user, barId, httpSession);
			List<BarWell> wells = sessionService.getWellsByBar(barId);
			return ResponseEntity.ok(wells);
		} catch (AccessDeniedException e) {
			return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
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
	public ResponseEntity<?> getSessionStatus(@PathVariable Long sessionId, @AuthenticationPrincipal User user,
			HttpSession httpSession) {
		try {
			InventorySession session = requireAccessibleSession(user, sessionId, httpSession);
			return ResponseEntity.ok(Map.of("sessionId", session.getSessionId(), "status", session.getStatus(), "barId",
					session.getBar().getBarId()));
		} catch (AccessDeniedException e) {
			return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	private Long getActiveBarId(HttpSession httpSession) {
		Object value = httpSession.getAttribute(ACTIVE_BAR_SESSION_KEY);
		if (value == null) {
			return null;
		}
		if (value instanceof Long longValue) {
			return longValue;
		}
		if (value instanceof Integer integerValue) {
			return integerValue.longValue();
		}
		return Long.valueOf(value.toString());
	}

	private boolean hasFreeAccess(HttpSession httpSession) {
		Object value = httpSession.getAttribute(ACTIVE_BAR_ACCESS_MODE_KEY);
		return FREE_ACCESS_MODE.equals(value);
	}

	private void clearActiveBarSelection(HttpSession httpSession) {
		httpSession.removeAttribute(ACTIVE_BAR_SESSION_KEY);
		httpSession.removeAttribute(ACTIVE_BAR_ACCESS_MODE_KEY);
	}

	private String redirectToSelectBar(RedirectAttributes redirectAttributes, HttpSession httpSession, String message) {
		clearActiveBarSelection(httpSession);
		redirectAttributes.addFlashAttribute("barError", message);
		return "redirect:/select-bar";
	}

	private Bar requireSelectedBar(User user, Long barId, HttpSession httpSession) {
		if (user == null) {
			throw new AccessDeniedException("Please login again.");
		}
		if (user.isAdmin()) {
			return barService.getBarById(barId);
		}

		Long activeBarId = getActiveBarId(httpSession);
		if (activeBarId == null) {
			throw new AccessDeniedException("Please select a bar before continuing.");
		}

		barService.validateSelectedBarAccess(user, activeBarId, barId, hasFreeAccess(httpSession));
		return barService.getBarById(barId);
	}

	private InventorySession requireAccessibleSession(User user, Long sessionId, HttpSession httpSession) {
		InventorySession inventorySession = sessionService.getSession(sessionId);
		requireSelectedBar(user, inventorySession.getBar().getBarId(), httpSession);
		return inventorySession;
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