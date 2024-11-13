package com.uranus.taskmanager.api.workspace.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KickWorkspaceMemberRequest {

	private String memberIdentifier;

	public KickWorkspaceMemberRequest(String memberIdentifier) {
		this.memberIdentifier = memberIdentifier;
	}
}
