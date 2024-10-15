package com.uranus.taskmanager.api.workspace.dto.request;

import lombok.Getter;

@Getter
public class WorkspaceParticipateRequest {
	/*
	 * Todo:
	 * 	- 비밀번호 필드(Optional)
	 * 	- 패턴 검증만 해도 될듯
	 */

	private String password;

	public WorkspaceParticipateRequest() {
	}

	public WorkspaceParticipateRequest(String password) {
		this.password = password;
	}
}
