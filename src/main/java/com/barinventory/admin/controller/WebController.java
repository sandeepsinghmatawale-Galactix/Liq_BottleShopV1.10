package com.barinventory.admin.controller;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.UserBarAccess;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.admin.repository.BarWellRepository;
import com.barinventory.admin.repository.UserBarAccessRepository;
import com.barinventory.admin.repository.UserRepository;
import com.barinventory.admin.service.BarService;
import com.barinventory.admin.service.BrandSizeProductService;
import com.barinventory.admin.service.InventorySessionService;
import com.barinventory.admin.service.PricingService;
import com.barinventory.admin.service.ProductService;
import com.barinventory.admin.service.ReportService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    private final BarService barService;
    private final ProductService productService;
    private final PricingService pricingService;
    private final InventorySessionService sessionService;
    private final ReportService reportService;

    private final UserRepository userRepository;
    private final BarRepository barRepository;
    private final BarWellRepository barWellRepository;
    private final BrandSizeProductService brandSizeProductService;
    private final UserBarAccessRepository userBarAccessRepository;

    // ================= GLOBAL ATTRIBUTES =================
    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal User user,
                                    HttpSession session,
                                    Model model) {
        if (user != null) {

            // Use repository or service to fetch bars
            List<Bar> userBars = userBarAccessRepository
                    .findByUser_IdAndActiveTrue(user.getId())
                    .stream()
                    .map(UserBarAccess::getBar)
                    .toList();

            model.addAttribute("currentUser", user);
            model.addAttribute("currentUserBars", userBars);
            model.addAttribute("bars", userBars); // for template
            model.addAttribute("activeBarId", session.getAttribute("activeBarId"));
            model.addAttribute("activeBarName", session.getAttribute("activeBarName"));
        }
    }

    // ================= LOGIN =================
    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    // ================= ROOT REDIRECT =================
    @GetMapping("/")
    public String redirectToDashboard(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    // ================= BAR SELECTOR =================
    @GetMapping("/select-bar")
    public String selectBar(Model model, @AuthenticationPrincipal User user) {
        if (user == null) return "redirect:/login";

        // ✅ Correctly fetch active bars for the user
        List<UserBarAccess> accesses =
                userBarAccessRepository.findByUser_IdAndActiveTrue(user.getId());

        model.addAttribute("barAccesses", accesses);
        return "select-bar";
    }

    @GetMapping("/select-bar/activate/{barId}")
    public String activateBar(@PathVariable Long barId,
                              @AuthenticationPrincipal User user,
                              HttpSession session) {

        if (user == null) return "redirect:/login";

        // ✅ Validate user has access
        barService.validateUserBarAccess(user, barId);

        Bar bar = barService.getBarById(barId);
        session.setAttribute("activeBarId", barId);
        session.setAttribute("activeBarName", bar.getBarName());

        return "redirect:/dashboard";
    }

    // ================= DASHBOARD =================
    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal User user,
                                HttpSession session,
                                Model model) {

        if (user == null) return "redirect:/login";

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("username", user.getName());
        model.addAttribute("role", user.getRole());

        if (user.isAdmin()) {
            List<Bar> bars = barRepository.findAll();
            model.addAttribute("bars", bars);
            model.addAttribute("totalBars", bars.size());
            model.addAttribute("totalUsers", userRepository.count());
            return "dashboard";
        }

        Long activeBarId = (Long) session.getAttribute("activeBarId");
        if (activeBarId == null) return "redirect:/select-bar";

        barService.validateUserBarAccess(user, activeBarId);
        Bar bar = barService.getBarById(activeBarId);
        model.addAttribute("bar", bar);
        model.addAttribute("barId", activeBarId);

        // ✅ Check if user is BAR OWNER
        boolean isOwner = userBarAccessRepository
                .findByBar_BarIdAndActiveTrue(activeBarId)
                .stream()
                .anyMatch(a -> a.getUser().getId().equals(user.getId())
                        && a.getBarRole() == Role.BAR_OWNER);

        if (isOwner) {
            long staffCount = userBarAccessRepository
                    .countByBar_BarIdAndBarRole(activeBarId, Role.BAR_STAFF);
            model.addAttribute("staffCount", staffCount);
        }

        return "dashboard";
    }

    // ================= HELPER =================
    private BigDecimal parseDecimal(String value) {
        return (value != null && !value.isEmpty())
                ? new BigDecimal(value)
                : BigDecimal.ZERO;
    }

    
}