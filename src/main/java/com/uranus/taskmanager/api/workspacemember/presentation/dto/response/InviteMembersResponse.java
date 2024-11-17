package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class InviteMembersResponse {

	private final List<InvitedMember> invitedMembers;
	private final List<FailedInvitedMember> failedInvitedMembers;

	// Todo: 초대한 사람 추가(Inviter)

	@Builder
	public InviteMembersResponse(List<InvitedMember> invitedMembers, List<FailedInvitedMember> failedInvitedMembers) {
		this.invitedMembers = invitedMembers;
		this.failedInvitedMembers = failedInvitedMembers;
	}
}
