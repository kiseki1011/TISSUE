package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.util.List;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class InviteMembersResponse {

	private final String workspaceCode;
	private final List<InvitedMember> invitedMembers;

	private InviteMembersResponse(String workspaceCode, List<InvitedMember> invitedMembers) {
		this.workspaceCode = workspaceCode;
		this.invitedMembers = invitedMembers;
	}

	public static InviteMembersResponse of(String workspaceCode, List<InvitedMember> invitedMembers) {
		return new InviteMembersResponse(workspaceCode, invitedMembers);
	}

	@Getter
	public static class InvitedMember {
		private final Long id;
		private final String email;

		private InvitedMember(Member member) {
			this.id = member.getId();
			this.email = member.getEmail();
		}

		public static InvitedMember from(Member member) {
			return new InvitedMember(member);
		}
	}
}
