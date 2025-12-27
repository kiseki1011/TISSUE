package com.tissue.workspace.adapter.in.web.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.workspace.application.dto.in.UpdateWorkspaceInfoCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceInfoRequest(

	JsonNullable<@Size(max = 100) @NotEmpty String> name,

	JsonNullable<@Size(max = 255) @NotEmpty String> description
) {
	public UpdateWorkspaceInfoCommand toCommand(String workspaceKey) {
		return new UpdateWorkspaceInfoCommand(workspaceKey, name, description);
	}
}
