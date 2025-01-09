package com.tissue.api.team.presentation.dto.request;

import com.tissue.api.common.enums.ColorType;

import jakarta.validation.constraints.NotNull;

public record UpdateTeamColorRequest(
	@NotNull
	ColorType colorType
) {
}
