package com.tissue.position.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.common.enums.ColorType;

public record UpdatePositionCommand(
	String workspaceKey,
	Long positionId,
	JsonNullable<String> name,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
