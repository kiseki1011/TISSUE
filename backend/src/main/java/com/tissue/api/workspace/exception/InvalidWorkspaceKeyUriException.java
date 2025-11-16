package com.tissue.api.workspace.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class InvalidWorkspaceKeyUriException extends BadRequestException {

	public InvalidWorkspaceKeyUriException(String uri) {
		super("Invalid workspace code in URI. URI: %s".formatted(uri));
	}
}
