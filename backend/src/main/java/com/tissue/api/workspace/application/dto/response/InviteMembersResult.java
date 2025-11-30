package com.tissue.api.workspace.application.dto.response;

import java.util.List;

import com.tissue.api.member.domain.model.Member;

public record InviteMembersResult(
	String workspaceKey,
	List<Long> invitedMemberIds
) {
	public static InviteMembersResult from(String workspaceKey, List<Member> invitedMembers) {
		return new InviteMembersResult(
			workspaceKey,
			invitedMembers.stream()
				.map(Member::getId)
				.toList()
		);
	}
}
