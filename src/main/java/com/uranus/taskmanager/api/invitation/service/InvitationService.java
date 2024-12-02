package com.uranus.taskmanager.api.invitation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
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
	public AcceptInvitationResponse acceptInvitation(Long memberId, Long invitationId) {

		Invitation invitation = getValidPendingInvitation(memberId, invitationId);
		WorkspaceMember workspaceMember = invitation.accept();

		workspaceMemberRepository.save(workspaceMember);

		return AcceptInvitationResponse.from(invitation);
	}

	@Transactional
	public RejectInvitationResponse rejectInvitation(Long memberId, Long invitationId) {

		Invitation invitation = getValidPendingInvitation(memberId, invitationId);
		invitation.reject();

		return RejectInvitationResponse.from(invitation);
	}

	private Invitation getValidPendingInvitation(Long memberId, Long invitationId) {
		Invitation invitation = invitationRepository
			.findById(invitationId)
			.orElseThrow(InvitationNotFoundException::new);

		String workspaceCode = invitation.getWorkspaceCode();
		invitationValidator.validateInvitation(memberId, workspaceCode);

		return invitation;
	}

}
