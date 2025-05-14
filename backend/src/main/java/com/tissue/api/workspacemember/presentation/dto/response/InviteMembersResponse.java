package com.tissue.api.workspacemember.presentation.dto.response;

import java.util.List;

import com.tissue.api.member.domain.model.Member;

public record InviteMembersResponse(
	String workspaceCode,
	List<Long> invitedMemberIds
) {

	public static InviteMembersResponse from(String workspaceCode, List<Member> invitedMembers) {
		return new InviteMembersResponse(workspaceCode,
			invitedMembers.stream()
				.map(Member::getId)
				.toList()
		);
	}
}
