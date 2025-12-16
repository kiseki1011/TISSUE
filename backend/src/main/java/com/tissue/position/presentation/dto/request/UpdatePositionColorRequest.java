package com.tissue.position.presentation.dto.request;

import com.tissue.common.enums.ColorType;

import jakarta.validation.constraints.NotNull;

public record UpdatePositionColorRequest(

	@NotNull(message = "{valid.notnull}")
	ColorType colorType
) {
}
