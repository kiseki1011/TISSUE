package com.uranus.taskmanager.fixture.entity;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

public class InvitationEntityFixture {

	public Invitation createPendingInvitation(Workspace workspace, Member member) {
		return Invitation.builder()
			.status(InvitationStatus.PENDING)
			.workspace(workspace)
			.member(member)
			.build();
	}

	public Invitation createAcceptedInvitation(Workspace workspace, Member member) {
		return Invitation.builder()
			.status(InvitationStatus.ACCEPTED)
			.workspace(workspace)
			.member(member)
			.build();
	}
}
