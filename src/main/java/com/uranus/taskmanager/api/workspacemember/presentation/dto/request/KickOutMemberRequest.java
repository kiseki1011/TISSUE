package com.uranus.taskmanager.api.workspacemember.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KickOutMemberRequest {

	private String memberIdentifier;

	public KickOutMemberRequest(String memberIdentifier) {
		this.memberIdentifier = memberIdentifier;
	}
}
