package com.tissue.api.team.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
	@NotBlank(message = "Team name is required")
	@Size(max = 50, message = "Team name must be at most 50 characters")
	String name,

	@NotBlank(message = "Team description is required")
	@Size(max = 200, message = "Team description must be at most 200 characters")
	String description
) {
}
