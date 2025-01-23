package com.tissue.api.workspace.presentation.dto.request;

import com.tissue.api.common.validator.annotation.pattern.SimplePasswordPattern;
import com.tissue.api.common.validator.annotation.size.password.SimplePasswordSize;

public record UpdateWorkspacePasswordRequest(
	String originalPassword,

	@SimplePasswordSize
	@SimplePasswordPattern
	String newPassword
) {
}
