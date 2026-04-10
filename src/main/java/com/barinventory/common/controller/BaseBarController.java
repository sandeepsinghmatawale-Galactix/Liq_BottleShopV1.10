package com.barinventory.common.controller;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.Role;
import com.barinventory.common.exception.BarAccessDeniedException;
import com.barinventory.common.exception.BarNotSelectedException;
import com.barinventory.common.service.InventorySessionService;

import jakarta.servlet.http.HttpSession;

public abstract class BaseBarController {

    protected static final String ACTIVE_BAR_ID = "ACTIVE_BAR_ID";
    protected static final String ACTIVE_BAR_ROLE = "ACTIVE_BAR_ROLE";

    // ==================== CORE ====================

    protected Long requireBar(HttpSession session) {
        Long barId = (Long) session.getAttribute(ACTIVE_BAR_ID);

        if (barId == null) {
            throw new BarNotSelectedException(); // ✅ custom exception
        }

        return barId;
    }

    protected Role requireRole(HttpSession session) {
        Role role = (Role) session.getAttribute(ACTIVE_BAR_ROLE);

        if (role == null) {
            throw new BarNotSelectedException();
        }

        return role;
    }

    // ==================== ROLE CHECKS ====================

    protected void requireOwner(HttpSession session) {
        if (requireRole(session) != Role.BAR_OWNER) {
            throw new BarAccessDeniedException("Owner access required");
        }
    }

    protected void requireStaff(HttpSession session) {
        if (requireRole(session) != Role.BAR_STAFF) {
            throw new BarAccessDeniedException("Staff access required");
        }
    }

    // ==================== ACCESS CONTROL ====================

    protected void validateBarAccess(Long barId, HttpSession session) {
        if (!requireBar(session).equals(barId)) {
            throw new BarAccessDeniedException("Invalid bar access");
        }
    }

    protected void validateSessionAccess(Long sessionId,
                                         HttpSession session,
                                         InventorySessionService service) {

        InventorySession inv = service.getSession(sessionId);

        if (inv == null) {
            throw new RuntimeException("Session not found");
        }

        validateBarAccess(inv.getBar().getBarId(), session);
    }
    
    protected void requireAdminRole(Role role) {
        if (role != Role.ADMIN) {
            throw new BarAccessDeniedException("Admin access required");
        }
    }
    
    
}