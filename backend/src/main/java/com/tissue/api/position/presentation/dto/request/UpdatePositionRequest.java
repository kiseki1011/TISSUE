package com.tissue.api.position.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePositionRequest(
	@NotBlank(message = "Position name is required")
	@Size(max = 50, message = "Position name must be at most 50 characters")
	String name,

	@Size(max = 200, message = "Position description must be at most 200 characters")
	String description
) {
}
