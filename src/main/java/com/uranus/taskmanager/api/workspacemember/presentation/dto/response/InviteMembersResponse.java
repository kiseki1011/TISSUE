package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.util.List;

import com.uranus.taskmanager.api.member.domain.Member;

public record InviteMembersResponse(
	String workspaceCode,
	List<InvitedMember> invitedMembers
) {

	public static InviteMembersResponse of(String workspaceCode, List<InvitedMember> invitedMembers) {
		return new InviteMembersResponse(workspaceCode, invitedMembers);
	}

	public record InvitedMember(Long id, String email) {
		public static InvitedMember from(Member member) {
			return new InvitedMember(member.getId(), member.getEmail());
		}
	}
}
