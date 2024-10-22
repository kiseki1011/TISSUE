package com.uranus.taskmanager.api.workspace.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class WorkspaceParticipateRequest {

	/**
	 * Todo
	 * 	- 비밀번호 필드 패턴 검증
	 */
	private String password;

	public WorkspaceParticipateRequest(String password) {
		this.password = password;
	}
}
