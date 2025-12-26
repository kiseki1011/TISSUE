package com.tissue.workspace.application.dto.in;

import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkspaceInfoCommand(
	String workspaceKey,
	JsonNullable<String> name,
	JsonNullable<String> description
) {
}
