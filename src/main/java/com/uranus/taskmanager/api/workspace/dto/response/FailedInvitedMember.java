package com.uranus.taskmanager.api.workspace.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class FailedInvitedMember {
	private final String identifier;
	private final String error;

	@Builder
	public FailedInvitedMember(String identifier, String error) {
		this.identifier = identifier;
		this.error = error;
	}
}
