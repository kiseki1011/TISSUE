package com.tissue.api.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequest(
	@NotBlank(message = "{valid.notblank}")
	String password
) {
}
