package com.uranus.taskmanager.api.invitation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.repository.InvitationRepository;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.RejectInvitationResponse;
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
	public AcceptInvitationResponse acceptInvitation(Long memberId, String workspaceCode) {

		Invitation invitation = getValidPendingInvitation(memberId, workspaceCode);
		WorkspaceMember workspaceMember = invitation.accept();

		workspaceMemberRepository.save(workspaceMember);

		return AcceptInvitationResponse.from(invitation, workspaceCode);
	}

	@Transactional
	public RejectInvitationResponse rejectInvitation(Long memberId, String workspaceCode) {

		Invitation invitation = getValidPendingInvitation(memberId, workspaceCode);
		invitation.reject();

		return RejectInvitationResponse.from(invitation, workspaceCode);
	}

	private Invitation getValidPendingInvitation(Long memberId, String workspaceCode) {
		Invitation invitation = invitationRepository
			.findByStatusAndWorkspaceCodeAndMemberId(InvitationStatus.PENDING, workspaceCode, memberId)
			.orElseThrow(InvitationNotFoundException::new);

		invitationValidator.validateInvitation(memberId, workspaceCode);
		return invitation;
	}

}
