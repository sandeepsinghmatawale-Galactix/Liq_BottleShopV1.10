package com.barinventory.owner.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.common.controller.BaseBarController;
import com.barinventory.common.service.InventorySessionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Controller
@RequestMapping("/owner")
@RequiredArgsConstructor
@Slf4j
public class OwnerDashboardController extends BaseBarController {
    
    private final BarRepository barRepository;
    private final InventorySessionService sessionService;
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user,
                           HttpSession httpSession,
                           Model model) {
        requireOwnerRole(httpSession);
        
        Long barId = getActiveBarId(httpSession);
        Bar bar = barRepository.findById(barId).orElseThrow();
        
        model.addAttribute("username", user.getName());
        model.addAttribute("bar", bar);
        model.addAttribute("recentSessions", sessionService.getRecentSessions(barId, 10));
        
        return "owner/dashboard";
    }
}