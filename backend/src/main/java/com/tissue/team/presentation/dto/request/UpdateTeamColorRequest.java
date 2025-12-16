package com.tissue.team.presentation.dto.request;

import com.tissue.common.enums.ColorType;

import jakarta.validation.constraints.NotNull;

public record UpdateTeamColorRequest(

	@NotNull(message = "{valid.notnull}")
	ColorType colorType
) {
}
