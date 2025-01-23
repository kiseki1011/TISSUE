package com.tissue.api.position.presentation.dto.request;

import com.tissue.api.common.enums.ColorType;

import jakarta.validation.constraints.NotNull;

public record UpdatePositionColorRequest(
	@NotNull(message = "{valid.notnull}")
	ColorType colorType
) {
}
