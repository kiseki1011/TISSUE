package com.tissue.workspace.application.dto.response;

import java.util.List;

import com.tissue.member.domain.Member;

public record InviteMembersResponse(
	String workspaceKey,
	List<String> invitedEmails,
	List<String> skippedEmails
) {
	public static InviteMembersResponse from(
		String workspaceKey,
		List<Member> invitedMembers,
		List<Member> skippedMembers
	) {
		return new InviteMembersResponse(
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
