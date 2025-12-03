package com.tissue.api.workspace.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;
import com.tissue.api.workspace.domain.enums.InvitationStatus;

public class InvitationAlreadyProcessedException extends BadRequestException {

	public InvitationAlreadyProcessedException(Long invitationId, InvitationStatus status) {
		super("Invitation (id: '%d') is already processed. Curent status: '%s'.".formatted(invitationId, status));
		addContext("invitationId", invitationId);
		addContext("status", status);
	}
}
