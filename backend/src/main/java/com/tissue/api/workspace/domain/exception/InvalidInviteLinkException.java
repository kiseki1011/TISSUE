package com.tissue.api.workspace.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class InvalidInviteLinkException extends BadRequestException {

	public InvalidInviteLinkException(String workspaceKey, String token) {
		super("Link for workspace '%s' is expired or inactive.".formatted(workspaceKey));
		addContext("workspaceKey", workspaceKey);
		addContext("token", token);
	}
}
