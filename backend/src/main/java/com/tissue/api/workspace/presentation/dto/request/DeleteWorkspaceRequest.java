package com.tissue.api.workspace.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ShortText;

public record DeleteWorkspaceRequest(

	@ShortText
	String password
) {
}
