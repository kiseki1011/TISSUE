package com.uranus.taskmanager.api.workspace.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class WorkspaceJoinRequest {

	/**
	 * Todo
	 * 	- 비밀번호 필드 패턴 검증
	 */
	private String password;

	public WorkspaceJoinRequest(String password) {
		this.password = password;
	}
}
