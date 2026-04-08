package com.barinventory.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barinventory.admin.dto.BarSubscriptionRequest;
import com.barinventory.admin.entity.Bar;
import com.barinventory.common.service.BarService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bars")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BarController {

    private final BarService barService;

    @GetMapping
    public ResponseEntity<List<Bar>> getAllBars() {
        return ResponseEntity.ok(barService.getAllActiveBars());
    }

    @GetMapping("/{barId}")
    public ResponseEntity<Bar> getBar(@PathVariable Long barId) {
        return ResponseEntity.ok(barService.getBarById(barId));
    }

    @PostMapping
    public ResponseEntity<Bar> createBar(@RequestBody Bar bar) {
        return ResponseEntity.ok(barService.createBar(bar));
    }

    @PutMapping("/{barId}")
    public ResponseEntity<Bar> updateBar(@PathVariable Long barId, @RequestBody Bar bar) {
        return ResponseEntity.ok(barService.updateBar(barId, bar));
    }

    @DeleteMapping("/{barId}")
    public ResponseEntity<Void> deactivateBar(@PathVariable Long barId) {
        barService.deactivateBar(barId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{barId}/subscription")
    public ResponseEntity<Bar> updateSubscription(@PathVariable Long barId,
            @RequestBody BarSubscriptionRequest request) {
        return ResponseEntity.ok(barService.updateSubscription(barId, request));
    }

    @PostMapping("/{barId}/subscription/renew")
    public ResponseEntity<Bar> renewSubscription(@PathVariable Long barId,
            @RequestBody BarSubscriptionRequest request) {
        return ResponseEntity.ok(barService.renewSubscription(barId, request));
    }

    @PostMapping("/{barId}/subscription/block")
    public ResponseEntity<Bar> blockBar(@PathVariable Long barId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(barService.blockBar(barId, notes));
    }

    @PostMapping("/{barId}/subscription/unblock")
    public ResponseEntity<Bar> unblockBar(@PathVariable Long barId) {
        return ResponseEntity.ok(barService.unblockBar(barId));
    }

    @PostMapping("/{barId}/hide")
    public ResponseEntity<Bar> hideBar(@PathVariable Long barId) {
        return ResponseEntity.ok(barService.hideBar(barId));
    }

    @PostMapping("/{barId}/unhide")
    public ResponseEntity<Bar> unhideBar(@PathVariable Long barId) {
        return ResponseEntity.ok(barService.unhideBar(barId));
    }
}
