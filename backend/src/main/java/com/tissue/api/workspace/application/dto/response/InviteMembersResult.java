package com.tissue.api.workspace.application.dto.response;

import java.util.List;

import com.tissue.api.member.domain.model.Member;

public record InviteMembersResult(
	String workspaceKey,
	List<String> invitedEmails,
	List<String> skippedEmails
) {
	public static InviteMembersResult from(
		String workspaceKey,
		List<Member> invitedMembers,
		List<Member> skippedMembers
	) {
		return new InviteMembersResult(
			workspaceKey,
			invitedMembers.stream()
				.map(Member::getEmail)
				.toList(),
			skippedMembers.stream()
				.map(Member::getEmail)
				.toList()
		);
	}
}
