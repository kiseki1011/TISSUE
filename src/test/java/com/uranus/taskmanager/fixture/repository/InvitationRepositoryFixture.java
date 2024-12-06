package com.uranus.taskmanager.fixture.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

@Component
@Transactional
public class InvitationRepositoryFixture {
	@Autowired
	private InvitationRepository invitationRepository;

	public Invitation createAndSaveInvitation(Workspace workspace, Member member, InvitationStatus invitationStatus) {
		Invitation invitation = Invitation.builder()
			.workspace(workspace)
			.member(member)
			.status(invitationStatus)
			.build();

		return invitationRepository.save(invitation);
	}
}
