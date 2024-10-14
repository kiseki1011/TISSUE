package com.uranus.taskmanager.api.workspace.dto.request;

import java.util.List;

import lombok.Getter;

/**
 * Todo: 리스트 대신 Set을 사용해서 중복 식별자(loginId, email)를 제거하는 것을 고려
 */
@Getter
public class InviteMembersRequest {

	// private final List<String> memberIdentifiers;
	private List<String> memberIdentifiers;

	public InviteMembersRequest() {
	}

	public InviteMembersRequest(List<String> memberIdentifiers) {
		this.memberIdentifiers = memberIdentifiers;
	}
}
