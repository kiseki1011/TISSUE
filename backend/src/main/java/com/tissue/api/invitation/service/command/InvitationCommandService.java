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
import com.tissue.api.util.RandomNicknameGenerator;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationCommandService {

	private final InvitationQueryService invitationQueryService;
	private final InvitationRepository invitationRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationValidator invitationValidator;
	private final RandomNicknameGenerator randomNicknameGenerator;

	@Transactional
	public AcceptInvitationResponse acceptInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = getPendingInvitation(memberId, invitationId);
		invitation.updateStatus(InvitationStatus.ACCEPTED);

		// Todo: nickname 유일성을 위한 처리 필요(동시성, 유일성 처리)
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(
			invitation.getMember(),
			invitation.getWorkspace(),
			WorkspaceRole.MEMBER,
			randomNicknameGenerator.generateNickname()
		);

		workspaceMemberRepository.save(workspaceMember);

		return AcceptInvitationResponse.from(invitation);
	}

	@Transactional
	public RejectInvitationResponse rejectInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = getPendingInvitation(memberId, invitationId);
		invitation.updateStatus(InvitationStatus.REJECTED);

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
		Invitation invitation = invitationQueryService.findPendingInvitation(invitationId);
		String workspaceCode = invitation.getWorkspaceCode();

		invitationValidator.validateInvitation(memberId, workspaceCode);

		return invitation;
	}
}
