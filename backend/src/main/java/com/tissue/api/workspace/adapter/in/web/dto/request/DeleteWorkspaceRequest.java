package com.tissue.api.workspace.adapter.in.web.dto.request;

import com.tissue.api.common.validator.annotation.size.text.ShortText;

public record DeleteWorkspaceRequest(

	@ShortText
	String password
) {
}
