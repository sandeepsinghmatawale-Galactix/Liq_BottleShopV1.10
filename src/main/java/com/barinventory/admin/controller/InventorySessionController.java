package com.barinventory.admin.controller;

import com.barinventory.admin.entity.DistributionRecord;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.StockroomInventory;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.WellInventory;
import com.barinventory.admin.service.BarService;
import com.barinventory.admin.service.InventorySessionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class InventorySessionController {

    private static final String ACTIVE_BAR_SESSION_KEY = "activeBarId";
    private static final String ACTIVE_BAR_ACCESS_MODE_KEY = "activeBarAccessMode";
    private static final String FREE_ACCESS_MODE = "FREE";

    private final InventorySessionService sessionService;
    private final BarService barService;

    @PostMapping("/create")
    public ResponseEntity<?> createSession(@RequestParam Long barId,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireSelectedBar(user, barId, httpSession);
            return ResponseEntity.ok(sessionService.createSession(barId));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            return ResponseEntity.ok(requireAccessibleSession(user, sessionId, httpSession));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/bar/{barId}")
    public ResponseEntity<?> getSessionsByBar(@PathVariable Long barId,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireSelectedBar(user, barId, httpSession);
            return ResponseEntity.ok(sessionService.getSessionsByBar(barId));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/stockroom")
    public ResponseEntity<Map<String, String>> saveStockroom(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireAccessibleSession(user, sessionId, httpSession);
            sessionService.saveStockroomFromForm(sessionId, formData);
            return ResponseEntity.ok(Map.of("message", "Stockroom saved"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/stockroom")
    public ResponseEntity<?> getStockroom(@PathVariable Long sessionId,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireAccessibleSession(user, sessionId, httpSession);
            return ResponseEntity.ok(sessionService.getStockroomBySession(sessionId));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/distribution")
    public ResponseEntity<Map<String, String>> saveDistribution(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireAccessibleSession(user, sessionId, httpSession);
            sessionService.saveDistributionAllocations(sessionId, formData);
            return ResponseEntity.ok(Map.of("message", "Distribution saved"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/distribution")
    public ResponseEntity<?> getDistribution(@PathVariable Long sessionId,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireAccessibleSession(user, sessionId, httpSession);
            return ResponseEntity.ok(sessionService.getDistributionsBySession(sessionId));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/wells")
    public ResponseEntity<Map<String, String>> saveWells(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireAccessibleSession(user, sessionId, httpSession);
            sessionService.saveWellInventoryFromForm(sessionId, formData);
            return ResponseEntity.ok(Map.of("message", "Wells saved"));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/wells")
    public ResponseEntity<?> getWells(@PathVariable Long sessionId,
            @AuthenticationPrincipal User user,
            HttpSession httpSession) {
        try {
            requireAccessibleSession(user, sessionId, httpSession);
            return ResponseEntity.ok(sessionService.getWellsBySession(sessionId));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

    private InventorySession requireAccessibleSession(User user, Long sessionId, HttpSession httpSession) {
        InventorySession inventorySession = sessionService.getSession(sessionId);
        requireSelectedBar(user, inventorySession.getBar().getBarId(), httpSession);
        return inventorySession;
    }

    private void requireSelectedBar(User user, Long requestedBarId, HttpSession httpSession) {
        if (user == null) {
            throw new AccessDeniedException("Please login again.");
        }

        if (user.isAdmin()) {
            barService.getBarById(requestedBarId);
            return;
        }

        Long activeBarId = getActiveBarId(httpSession);
        if (activeBarId == null) {
            throw new AccessDeniedException("Please select a bar before continuing.");
        }

        barService.validateSelectedBarAccess(user, activeBarId, requestedBarId, hasFreeAccess(httpSession));
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
        return FREE_ACCESS_MODE.equals(httpSession.getAttribute(ACTIVE_BAR_ACCESS_MODE_KEY));
    }
}
