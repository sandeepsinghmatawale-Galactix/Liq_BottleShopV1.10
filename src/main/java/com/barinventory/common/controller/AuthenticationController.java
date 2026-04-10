package com.barinventory.common.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.entity.UserBarAccess;
import com.barinventory.admin.repository.UserBarAccessRepository;
import com.barinventory.common.exception.BarAccessDeniedException;
import com.barinventory.common.service.BarService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    private final BarService barService;
    private final UserBarAccessRepository userBarAccessRepository;

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

   
    @PostMapping("/select-bar")
    public String selectBar(@AuthenticationPrincipal User user,
                           @RequestParam Long barId,
                           HttpSession session) {
        UserBarAccess access = userBarAccessRepository
            .findByUser_IdAndBar_BarIdAndActiveTrueAndBar_ActiveTrue(user.getId(), barId)
            .orElseThrow(() -> new BarAccessDeniedException("No access to this bar"));

        session.setAttribute("ACTIVE_BAR_ID", barId);
        session.setAttribute("ACTIVE_BAR_ROLE", access.getBarRole());

        return redirectToRoleDashboard(access.getBarRole());
    }

    // ✅ ADD THESE METHODS
    private Role getBarRole(HttpSession session) {
        Object value = session.getAttribute("ACTIVE_BAR_ROLE");
        return value instanceof Role ? (Role) value : null;
    }

    private String redirectToRoleDashboard(Role barRole) {
        return switch(barRole) {
            case BAR_OWNER -> "redirect:/owner/dashboard";
            case BAR_STAFF -> "redirect:/staff/dashboard";
            default -> "redirect:/select-bar";
        };
    }
}