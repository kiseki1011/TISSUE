package com.tissue.workspace.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class InvalidWorkspaceKeyUriException extends BadRequestException {

	public InvalidWorkspaceKeyUriException(String uri) {
		super("Invalid workspace code in URI. URI: %s".formatted(uri));
	}
}
