package com.tissue.api.invitation.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;
import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.tissue.api.invitation.presentation.dto.response.RejectInvitationResponse;
import com.tissue.api.invitation.validator.InvitationValidator;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationCommandService {

	private final InvitationRepository invitationRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationValidator invitationValidator;

	@Transactional
	public AcceptInvitationResponse acceptInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = getPendingInvitation(memberId, invitationId);
		WorkspaceMember workspaceMember = invitation.accept();

		workspaceMemberRepository.save(workspaceMember);

		return AcceptInvitationResponse.from(invitation);
	}

	@Transactional
	public RejectInvitationResponse rejectInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = getPendingInvitation(memberId, invitationId);
		invitation.reject();

		return RejectInvitationResponse.from(invitation);
	}

	@Transactional
	public void deleteInvitations(
		Long memberId
	) {
		invitationRepository.deleteAllByMemberIdAndStatusIn(
			memberId,
			List.of(InvitationStatus.ACCEPTED, InvitationStatus.REJECTED)
		);
	}

	private Invitation getPendingInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = findInvitation(invitationId);

		String workspaceCode = invitation.getWorkspaceCode();
		invitationValidator.validateInvitation(memberId, workspaceCode);

		return invitation;
	}

	private Invitation findInvitation(Long invitationId) {
		return invitationRepository.findById(invitationId)
			.orElseThrow(() ->
				new ResourceNotFoundException(String.format("Invitation not found with id: %d", invitationId)));
	}
}
