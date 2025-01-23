package com.tissue.api.security.authentication.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Todo
 *  - NotBlank, Size 검증 필요
 */
@Builder
public record LoginRequest(
	@NotBlank(message = "{valid.notblank}")
	String identifier,

	@NotBlank(message = "{valid.notblank}")
	String password
) {
}
