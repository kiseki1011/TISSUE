package com.tissue.api.workspacemember.presentation.dto.request;

public record JoinWorkspaceRequest(
	/*
	 * Todo
	 * 	- 비밀번호 필드 패턴 검증
	 */
	String password
) {
}
