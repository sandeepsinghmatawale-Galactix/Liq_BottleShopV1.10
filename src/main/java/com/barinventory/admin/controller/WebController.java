package com.barinventory.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.barinventory.admin.entity.*;
import com.barinventory.admin.repository.*;
import com.barinventory.admin.service.*;

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
                && !(authentication.getPrincipal().equals("anonymousUser"))) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal User currentUser, Model model) {

        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("username", currentUser.getName());
        model.addAttribute("role", currentUser.getRole());

        if (currentUser.getRole() == Role.ADMIN) {
            List<Bar> bars = barRepository.findAll();
            model.addAttribute("bars", bars);
            model.addAttribute("totalBars", bars.size());
            model.addAttribute("totalUsers", userRepository.count());
            return "dashboard";
        }

        Long barId = currentUser.getBarId();
        if (barId == null) {
            return "redirect:/login?error=bar_not_assigned";
        }

        Bar bar = barService.getBarById(barId);
        if (bar == null) {
            return "redirect:/login?error=invalid_bar";
        }

        model.addAttribute("bar", bar);
        model.addAttribute("barId", barId);

        if (currentUser.getRole() == Role.BAR_OWNER) {
            long staffCount = userRepository.countByBar_BarIdAndRole(barId, Role.BAR_STAFF);
            model.addAttribute("staffCount", staffCount);
        }

        return "dashboard";
    }

    // ================= BILLING =================

    
    
    @GetMapping("/billing")
    public String billingPage(Model model) {
        model.addAttribute("activePage", "billing");
        return "billing";
    }

    // ================= INVOICES =================

    
    // ================= ADMIN BAR REGISTER =================

    @GetMapping("/admin/bars/register")
    public String showRegisterBar(Model model) {
        model.addAttribute("barTypes", List.of("STANDALONE", "HOTEL_BAR", "RESTAURANT_BAR", "CLUB", "OTHER"));
        model.addAttribute("shiftConfigs", List.of("SINGLE", "DOUBLE"));
        return "admin/bar-register";
    }

    @PostMapping("/admin/bars/register")
    public String saveRegisterBar(@RequestParam Map<String, String> form) {

        Bar bar = Bar.builder()
                .barName(form.get("barName"))
                .barType(form.get("barType"))
                .ownerName(form.get("ownerName"))
                .contactNumber(form.get("contactNumber"))
                .email(form.get("email"))
                .city(form.get("city"))
                .active(false)
                .build();

        Bar saved = barService.createBar(bar);
        return "redirect:/admin/bars/" + saved.getBarId() + "/wells-config";
    }

    // ================= STOCKROOM =================

    @GetMapping("/stockroom/{sessionId}")
    public String viewStockroom(@PathVariable Long sessionId, Model model) {

        InventorySession inv = sessionService.getSession(sessionId);
        List<Product> products = productService.getAllActiveProducts();

        model.addAttribute("inv", inv);
        model.addAttribute("products", products);

        return "stockroom";
    }

    @PostMapping("/stockroom/{sessionId}")
    public String saveStockroom(@PathVariable Long sessionId,
                               @RequestParam Map<String, String> formData,
                               Model model) {

        try {
            InventorySession inv = sessionService.getSession(sessionId);
            List<Product> products = productService.getAllActiveProducts();
            List<StockroomInventory> inventories = new ArrayList<>();

            for (Product product : products) {

                BigDecimal opening = parseDecimal(formData.get("opening_" + product.getProductId()));
                BigDecimal received = parseDecimal(formData.get("received_" + product.getProductId()));
                BigDecimal closing = parseDecimal(formData.get("closing_" + product.getProductId()));

                StockroomInventory inventory = StockroomInventory.builder()
                        .session(inv)
                        .product(product)
                        .openingStock(opening)
                        .receivedStock(received)
                        .closingStock(closing)
                        .build();

                inventories.add(inventory);
            }

            sessionService.saveStockroomInventory(sessionId, inventories);

            return "redirect:/dashboard";

        } catch (Exception e) {
            log.error("Error saving stockroom", e);
            model.addAttribute("error", e.getMessage());
            return "stockroom";
        }
    }

    // ================= PRODUCTS =================

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllActiveProducts());
        return "list";
    }

    @GetMapping("/products/new")
    public String newProductForm() {
        return "productNew";
    }

    @PostMapping("/products/new")
    public String createProduct(@RequestParam String productName,
                               @RequestParam String category) {

        Product product = Product.builder()
                .productName(productName)
                .category(category)
                .active(true)
                .build();

        productService.createProduct(product);
        return "redirect:/products";
    }

    // ================= REPORT =================

    @GetMapping("/reports/{barId}/daily")
    public String dailyReport(@PathVariable Long barId,
                             @RequestParam(required = false) String date,
                             Model model) {

        LocalDateTime reportDate =
                date != null ? LocalDateTime.parse(date) : LocalDateTime.now();

        model.addAttribute("report",
                reportService.getDailySalesReport(barId, reportDate));

        return "reports/daily";
    }

    // ================= HELPER =================

    private BigDecimal parseDecimal(String value) {
        return (value != null && !value.isEmpty())
                ? new BigDecimal(value)
                : BigDecimal.ZERO;
    }
}