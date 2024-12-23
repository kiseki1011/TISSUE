package com.tissue.api.workspace.presentation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateWorkspaceInfoRequest {

	@Size(min = 2, max = 50, message = "Workspace name must be 2 ~ 50 characters long")
	private String name;
	@Size(min = 1, max = 255, message = "Workspace description must be 1 ~ 255 characters long")
	private String description;

	@Builder
	public UpdateWorkspaceInfoRequest(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public boolean hasName() {
		return isNotBlank(name);
	}

	public boolean hasDescription() {
		return isNotBlank(description);
	}

	private boolean isNotBlank(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
