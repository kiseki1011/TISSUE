package com.tissue.api.workspace.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteWorkspaceRequest(
	@NotBlank(message = "{valid.notblank}")
	String password
) {
}
