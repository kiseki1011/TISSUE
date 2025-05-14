package com.tissue.api.invitation.presentation.controller.query;

import java.util.List;

import com.tissue.api.invitation.domain.enums.InvitationStatus;

public record InvitationSearchCondition(
	List<InvitationStatus> statuses
) {
	public InvitationSearchCondition {
		if (statuses == null || statuses.isEmpty()) {
			statuses = List.of(InvitationStatus.PENDING);
		}
	}

	public InvitationSearchCondition() {
		this(List.of(InvitationStatus.PENDING));
	}
}
