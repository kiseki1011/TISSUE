package com.tissue.api.workspace.adapter.in.web.dto.request;

import com.tissue.api.common.validator.annotation.pattern.SimplePasswordPattern;
import com.tissue.api.common.validator.annotation.size.password.SimplePasswordSize;
import com.tissue.api.common.validator.annotation.size.text.ShortText;

public record UpdateWorkspacePasswordRequest(

	@ShortText
	String originalPassword,

	@SimplePasswordSize
	@SimplePasswordPattern
	String newPassword
) {
}
