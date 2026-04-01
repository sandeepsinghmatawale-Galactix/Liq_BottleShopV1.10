package com.barinventory.admin.controller;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.service.InventorySessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class InventorySessionController {

    private final InventorySessionService sessionService;

    // ✅ CREATE SESSION (SETUP)
    @PostMapping("/initialize")
    public ResponseEntity<InventorySession> initializeSession(
            @RequestParam Long barId) {

        return ResponseEntity.ok(sessionService.createSetupSession(barId));
    }

    // ✅ SAVE STOCKROOM (FORM-BASED)
    @PostMapping("/{sessionId}/stockroom")
    public ResponseEntity<Map<String, String>> saveStockroom(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData) {

        sessionService.saveSetupStockroom(sessionId, formData);

        return ResponseEntity.ok(Map.of("message", "Stockroom inventory saved"));
    }

    // ❌ REMOVED: createDistributionRecords (not in service)

    // ✅ SAVE WELLS (FORM-BASED)
    @PostMapping("/{sessionId}/wells")
    public ResponseEntity<Map<String, String>> saveWells(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> formData) {

        sessionService.saveSetupWells(sessionId, formData);

        return ResponseEntity.ok(Map.of("message", "Well inventory saved"));
    }

    // ✅ FINALIZE SESSION
    @PostMapping("/{sessionId}/commit")
    public ResponseEntity<Map<String, String>> commitSession(@PathVariable Long sessionId) {

        try {
            sessionService.finalizeSetupSession(sessionId);
            return ResponseEntity.ok(Map.of("message", "Session committed successfully"));

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ ROLLBACK
    @PostMapping("/{sessionId}/rollback")
    public ResponseEntity<Map<String, String>> rollbackSession(
            @PathVariable Long sessionId,
            @RequestParam String reason) {

        sessionService.rollbackSession(sessionId, reason);

        return ResponseEntity.ok(Map.of("message", "Session rolled back"));
    }

    // ✅ GET SESSION
    @GetMapping("/{sessionId}")
    public ResponseEntity<InventorySession> getSession(@PathVariable Long sessionId) {

        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }
}