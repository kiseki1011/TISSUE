package com.tissue.api.workspace.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.common.validator.annotation.size.text.StandardText;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateWorkspaceInfoRequest(

	@NameSize
	@NotBlank(message = "{valid.notblank}")
	String name,

	@StandardText
	@NotBlank(message = "{valid.notblank}")
	String description
) {
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
