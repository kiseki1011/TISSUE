package com.tissue.fixture.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;
import com.tissue.api.invitation.domain.repository.InvitationRepository;

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
