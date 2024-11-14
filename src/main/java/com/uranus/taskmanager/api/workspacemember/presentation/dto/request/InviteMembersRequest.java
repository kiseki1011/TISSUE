package com.uranus.taskmanager.api.workspacemember.presentation.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Todo: 리스트 대신 Set을 사용해서 중복 식별자(loginId, email)를 제거하는 것을 고려
 */
@ToString
@Getter
@NoArgsConstructor
public class InviteMembersRequest {

	private List<String> memberIdentifiers;

	public InviteMembersRequest(List<String> memberIdentifiers) {
		this.memberIdentifiers = memberIdentifiers;
	}
}
