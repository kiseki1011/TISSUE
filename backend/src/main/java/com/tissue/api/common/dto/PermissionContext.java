package com.tissue.api.common.dto;

import java.time.LocalDateTime;

public record PermissionContext(
	Boolean updatePermission,
	LocalDateTime expiresAt
) {
}
