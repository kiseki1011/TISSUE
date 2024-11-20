package com.uranus.taskmanager.api.invitation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.repository.InvitationRepository;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.validator.InvitationValidator;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService {

	private final InvitationRepository invitationRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationValidator invitationValidator;

	@Transactional
	public InvitationAcceptResponse acceptInvitation(Long memberId, String workspaceCode) {

		Invitation invitation = getValidInvitation(memberId, workspaceCode);

		WorkspaceMember workspaceMember = invitation.accept();

		workspaceMemberRepository.save(workspaceMember);

		return InvitationAcceptResponse.from(invitation.getWorkspace());
	}

	@Transactional
	public void rejectInvitation(Long memberId, String workspaceCode) {

		Invitation invitation = getValidInvitation(memberId, workspaceCode);

		invitation.reject();
	}

	private Invitation getValidInvitation(Long memberId, String workspaceCode) {
		Invitation invitation = invitationRepository
			.findByWorkspaceCodeAndMemberId(workspaceCode, memberId)
			.orElseThrow(InvitationNotFoundException::new);

		invitationValidator.validateInvitation(memberId, workspaceCode);
		return invitation;
	}

}
