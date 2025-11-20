package com.tissue.api.project.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import lombok.Builder;

@Builder
public record UpdateProjectCommand(
	String workspaceKey,
	String projectKey,
	JsonNullable<String> name,
	JsonNullable<String> description
) {
}
