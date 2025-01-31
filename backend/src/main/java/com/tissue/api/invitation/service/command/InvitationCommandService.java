package com.tissue.api.invitation.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;
import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.tissue.api.invitation.presentation.dto.response.RejectInvitationResponse;
import com.tissue.api.invitation.service.query.InvitationQueryService;
import com.tissue.api.invitation.validator.InvitationValidator;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationCommandService {

	private final InvitationQueryService invitationQueryService;
	private final InvitationRepository invitationRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationValidator invitationValidator;

	@Transactional
	public AcceptInvitationResponse acceptInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = getPendingInvitation(memberId, invitationId);

		// Todo: accept()에서 addCollaboratorWorkspaceMember의 책임은 누가?
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
		Invitation invitation = invitationQueryService.findInvitation(invitationId);
		String workspaceCode = invitation.getWorkspaceCode();

		invitationValidator.validateInvitation(memberId, workspaceCode);

		return invitation;
	}
}
