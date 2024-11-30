package com.uranus.taskmanager.api.workspacemember.presentation.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferOwnershipRequest {
	private String memberIdentifier;

	public TransferOwnershipRequest(String memberIdentifier) {
		this.memberIdentifier = memberIdentifier;
	}
}
