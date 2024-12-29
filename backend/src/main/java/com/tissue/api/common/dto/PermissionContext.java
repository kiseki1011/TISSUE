package com.tissue.api.common.dto;

import java.time.LocalDateTime;

import com.tissue.api.common.enums.PermissionType;

public record PermissionContext(
	PermissionType permissionType,
	Boolean permission,
	LocalDateTime expiresAt
) {
	public boolean permissionIsNull() {
		return permission == null;
	}

	public boolean permissionIsFalse() {
		return !permission;
	}

	public boolean expirationDateIsNull() {
		return expiresAt == null;
	}

	public boolean permissionExpired() {
		return LocalDateTime.now().isAfter(expiresAt);
	}
}
