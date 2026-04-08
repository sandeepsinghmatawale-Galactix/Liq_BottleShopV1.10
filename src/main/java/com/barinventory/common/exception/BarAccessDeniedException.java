package com.barinventory.common.exception;

import org.springframework.security.access.AccessDeniedException;

public class BarAccessDeniedException extends AccessDeniedException {

	public BarAccessDeniedException(String message) {
		super(message);
	}

	public BarAccessDeniedException(String message, Throwable cause) {
		super(message, cause);
	}
}