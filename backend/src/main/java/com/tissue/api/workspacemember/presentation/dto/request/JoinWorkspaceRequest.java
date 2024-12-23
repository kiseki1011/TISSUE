package com.tissue.api.workspacemember.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class JoinWorkspaceRequest {

	/**
	 * Todo
	 * 	- 비밀번호 필드 패턴 검증
	 */
	private String password;

	public JoinWorkspaceRequest(String password) {
		this.password = password;
	}
}
