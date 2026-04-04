package com.barinventory.admin.controller;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.DistributionRecord;
import com.barinventory.admin.entity.StockroomInventory;
import com.barinventory.admin.entity.WellInventory;
import com.barinventory.admin.service.InventorySessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class InventorySessionController {

    private final InventorySessionService sessionService;

    // =========================================================
    // SESSION
    // =========================================================

    @PostMapping("/create")
    public ResponseEntity<InventorySession> createSession(@RequestParam Long barId) {
        return ResponseEntity.ok(sessionService.createSession(barId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<InventorySession> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @GetMapping("/bar/{barId}")
    public ResponseEntity<List<InventorySession>> getSessionsByBar(@PathVariable Long barId) {
        return ResponseEntity.ok(sessionService.getSessionsByBar(barId));
    }

    // =========================================================
    // STOCKROOM
    // =========================================================

    @PostMapping("/{sessionId}/stockroom")
    public ResponseEntity<Map<String, String>> saveStockroom(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData) {

        sessionService.saveStockroomFromForm(sessionId, formData);

        return ResponseEntity.ok(Map.of("message", "Stockroom saved"));
    }

    @GetMapping("/{sessionId}/stockroom")
    public ResponseEntity<List<StockroomInventory>> getStockroom(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getStockroomBySession(sessionId));
    }

    // =========================================================
    // DISTRIBUTION
    // =========================================================

    @PostMapping("/{sessionId}/distribution")
    public ResponseEntity<Map<String, String>> saveDistribution(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData) {

        sessionService.saveDistributionAllocations(sessionId, formData);

        return ResponseEntity.ok(Map.of("message", "Distribution saved"));
    }

    @GetMapping("/{sessionId}/distribution")
    public ResponseEntity<List<DistributionRecord>> getDistribution(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getDistributionsBySession(sessionId));
    }

    // =========================================================
    // WELLS
    // =========================================================

    @PostMapping("/{sessionId}/wells")
    public ResponseEntity<Map<String, String>> saveWells(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData) {

        sessionService.saveWellInventoryFromForm(sessionId, formData);

        return ResponseEntity.ok(Map.of("message", "Wells saved"));
    }

    @GetMapping("/{sessionId}/wells")
    public ResponseEntity<List<WellInventory>> getWells(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getWellsBySession(sessionId));
    }
}