package com.barinventory.common.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
public class DashboardRouterController {

    private final BarService barService;
    private final UserBarAccessRepository userBarAccessRepository;

    @GetMapping("/")
    public String root(@AuthenticationPrincipal User user, HttpSession session) {
        if (user == null) return "redirect:/login";

        if (user.getRole() == Role.ADMIN) {
            return "redirect:/admin/dashboard";
        }

        List<Bar> bars = barService.getActiveBarsForUser(user);
        if (bars.size() == 1) {
            activateBar(user, bars.get(0).getBarId(), session);
            return redirectToRoleDashboard(getBarRole(session));
        }

        return "redirect:/select-bar";
    }

    @GetMapping("/dashboard")
    public String routeDashboard(@AuthenticationPrincipal User user,
                                HttpSession session) {
        if (user == null) return "redirect:/login";

        if (user.getRole() == Role.ADMIN) {
            return "redirect:/admin/dashboard";
        }

        Role barRole = getBarRole(session);
        if (barRole == null) return "redirect:/select-bar";

        return redirectToRoleDashboard(barRole);
    }

    @GetMapping("/switch-bar/{barId}")
    public String switchBar(@PathVariable Long barId,
                           @AuthenticationPrincipal User user,
                           HttpSession session) {
        activateBar(user, barId, session);
        return redirectToRoleDashboard(getBarRole(session));
    }

    // ✅ PRIVATE HELPER METHODS
    private void activateBar(User user, Long barId, HttpSession session) {
        UserBarAccess access = userBarAccessRepository
            .findByUser_IdAndBar_BarIdAndActiveTrueAndBar_ActiveTrue(user.getId(), barId)
            .orElseThrow(() -> new BarAccessDeniedException("No access"));

        session.setAttribute("ACTIVE_BAR_ID", barId);
        session.setAttribute("ACTIVE_BAR_ROLE", access.getBarRole());
    }

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