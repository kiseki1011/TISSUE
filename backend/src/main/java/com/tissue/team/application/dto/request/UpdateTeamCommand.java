package com.tissue.team.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.common.enums.ColorType;

public record UpdateTeamCommand(
	String workspaceKey,
	Long teamId,
	JsonNullable<String> name,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
