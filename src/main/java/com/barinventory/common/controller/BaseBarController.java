package com.barinventory.common.controller;

import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.Role;
import com.barinventory.common.exception.BarAccessDeniedException;
import com.barinventory.common.service.InventorySessionService;

import jakarta.servlet.http.HttpSession;

public abstract class BaseBarController {

	protected static final String ACTIVE_BAR_ID = "ACTIVE_BAR_ID";
	protected static final String ACTIVE_BAR_ROLE = "ACTIVE_BAR_ROLE";

	// ==================== GETTERS ====================

	protected Long getActiveBarId(HttpSession session) {
		Object value = session.getAttribute(ACTIVE_BAR_ID);
		if (value instanceof Long)
			return (Long) value;
		if (value instanceof Integer)
			return ((Integer) value).longValue();
		return null;
	}

	protected Role getBarRole(HttpSession session) {
		Object value = session.getAttribute(ACTIVE_BAR_ROLE);
		return value instanceof Role ? (Role) value : null;
	}

	// ==================== VALIDATORS ====================

	protected void requireOwnerRole(HttpSession session) {
		Role role = getBarRole(session);
		if (role != Role.BAR_OWNER) {
			throw new BarAccessDeniedException("Owner access required");
		}
	}

 

	protected void requireStaffRole(HttpSession session) {
		Role role = getBarRole(session);
		if (role != Role.BAR_STAFF) {
			throw new BarAccessDeniedException("Staff access required");
		}
	}

	protected void requireAdminRole(Role userRole) {
		if (userRole != Role.ADMIN) {
			throw new BarAccessDeniedException("Admin access required");
		}
	}

	protected void validateBarAccess(Long barId, HttpSession session) {
		Long activeBarId = getActiveBarId(session);
		if (activeBarId == null || !activeBarId.equals(barId)) {
			throw new BarAccessDeniedException("No access to this bar");
		}
	}

	protected void validateSessionAccess(Long sessionId, HttpSession session, InventorySessionService sessionService) {
		if (sessionService == null) {
			return; // Skip if service not provided
		}
		InventorySession inv = sessionService.getSession(sessionId);
		validateBarAccess(inv.getBar().getBarId(), session);
	}
}