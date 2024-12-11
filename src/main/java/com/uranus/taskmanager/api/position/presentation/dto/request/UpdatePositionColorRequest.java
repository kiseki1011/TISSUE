package com.uranus.taskmanager.api.position.presentation.dto.request;

import com.uranus.taskmanager.api.common.ColorType;

import jakarta.validation.constraints.NotNull;

public record UpdatePositionColorRequest(
	@NotNull
	ColorType colorType
) {
}
