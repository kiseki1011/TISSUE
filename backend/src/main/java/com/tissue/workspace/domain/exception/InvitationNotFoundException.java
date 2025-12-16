package com.tissue.workspace.domain.exception;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class InvitationNotFoundException extends ResourceNotFoundException {

	public InvitationNotFoundException(Long invitationId) {
		super("Invitation not found for id '%d'.".formatted(invitationId));
		addContext("invitationId", invitationId);
	}
}
