package com.tissue.api.workspace.presentation.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateWorkspacePasswordRequest(
	String originalPassword,
	@Pattern(
		regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric and must be between 4 and 30 characters"
	)
	String newPassword
) {
}
