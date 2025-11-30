package com.tissue.api.workspace.adapter.in.web.dto.request;

import com.tissue.api.workspace.application.dto.request.CreateWorkspaceCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(

	@Size(max = 100) @NotBlank String name,

	@Size(max = 255) @NotBlank String description
) {
	public CreateWorkspaceCommand toCommand(Long memberId) {
		return new CreateWorkspaceCommand(name, description, memberId);
	}
}
