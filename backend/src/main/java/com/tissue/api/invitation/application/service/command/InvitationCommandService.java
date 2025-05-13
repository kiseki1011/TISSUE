package com.tissue.api.invitation.application.service.command;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.invitation.application.service.reader.InvitationReader;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.enums.InvitationStatus;
import com.tissue.api.invitation.domain.service.InvitationValidator;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.invitation.presentation.dto.response.InvitationResponse;
import com.tissue.api.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationCommandService {

	private final InvitationReader invitationReader;
	private final InvitationRepository invitationRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationValidator invitationValidator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public InvitationResponse acceptInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = getPendingInvitation(memberId, invitationId);
		invitation.updateStatus(InvitationStatus.ACCEPTED);

		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(
			invitation.getMember(),
			invitation.getWorkspace()
		);

		workspaceMemberRepository.save(workspaceMember);

		eventPublisher.publishEvent(
			MemberJoinedWorkspaceEvent.createEvent(workspaceMember)
		);

		return InvitationResponse.from(invitation);
	}

	@Transactional
	public InvitationResponse rejectInvitation(
		Long memberId,
		Long invitationId
	) {
		Invitation invitation = getPendingInvitation(memberId, invitationId);
		invitation.updateStatus(InvitationStatus.REJECTED);

		return InvitationResponse.from(invitation);
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
		Invitation invitation = invitationReader.findPendingInvitation(invitationId);
		String workspaceCode = invitation.getWorkspaceCode();

		// TODO: InvitationValidator를 InvitationValidationService로 이름 바꾸기?
		//  - 또는 validation을 Invitation 엔티티에서 진행?
		//  - 그런데 다른 도메인과 연결되었기 때문에 도메인 서비스로 분리하는게 좋을 듯
		invitationValidator.validateInvitation(memberId, workspaceCode);

		return invitation;
	}
}
