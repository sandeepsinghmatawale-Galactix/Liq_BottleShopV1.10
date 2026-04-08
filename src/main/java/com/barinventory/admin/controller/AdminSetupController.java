package com.barinventory.admin.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.common.service.InventorySessionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/setup")
@RequiredArgsConstructor
public class AdminSetupController {
    
    private final InventorySessionService sessionService;
    
    @GetMapping("/{sessionId}/stockroom")
    public String showStockroom(@PathVariable Long sessionId, Model model) {
        InventorySession session = sessionService.getSession(sessionId);
        model.addAttribute("session", session);
        model.addAttribute("brandSizes", sessionService.getAllActiveBrandSizes());
        return "admin/setup/stockroom";
    }
    
    @PostMapping("/{sessionId}/stockroom")
    public String saveStockroom(@PathVariable Long sessionId,
                               @RequestParam Map<String, String> formData,
                               Model model) {
        try {
            sessionService.saveSetupStockroom(sessionId, formData);
            return "redirect:/admin/setup/" + sessionId + "/wells";
        } catch (Exception e) {
            InventorySession session = sessionService.getSession(sessionId);
            model.addAttribute("session", session);
            model.addAttribute("error", e.getMessage());
            return "admin/setup/stockroom";
        }
    }
}