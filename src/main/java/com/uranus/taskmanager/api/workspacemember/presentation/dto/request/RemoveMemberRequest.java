package com.uranus.taskmanager.api.workspacemember.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RemoveMemberRequest {

	private String memberIdentifier;

	public RemoveMemberRequest(String memberIdentifier) {
		this.memberIdentifier = memberIdentifier;
	}
}
