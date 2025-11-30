package com.tissue.api.invitation.application.service.command;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.invitation.application.service.finder.InvitationFinder;
import com.tissue.api.invitation.domain.enums.InvitationStatus;
import com.tissue.api.invitation.domain.service.InvitationValidator;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.invitation.presentation.dto.response.InvitationResponse;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.Invitation;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationCommandService {

	private final InvitationFinder invitationFinder;
	private final InvitationRepository invitationRepository;
	private final InvitationValidator invitationValidator;
	private final WorkspacePolicy workspacePolicy;
	private final ApplicationEventPublisher eventPublisher;

	// TODO: 전체적으로 Invitation에 대한 방식도 수정할 필요가 있음
	//  - 엔티티 수정(연관 관계 개선)
	//  - role을 정해서 초대를 보낼 수 있도록 변경
	@Transactional
	public InvitationResponse acceptInvitation(
		Long memberId,
		Long invitationId
	) {
		// TODO: member를 조회하고, 해당 member를 통해 invitation 조회로 방식을 변경?
		Invitation invitation = getPendingInvitation(memberId, invitationId);
		invitation.updateStatus(InvitationStatus.ACCEPTED);

		Workspace workspace = invitation.getWorkspace();
		Member member = invitation.getMember();

		// memberPolicy.ensureCanJoin(member);
		// workspacePolicy.ensureCanAddMember(workspace);

		WorkspaceMember workspaceMember = workspace.addMember(member, WorkspaceRole.MEMBER);

		// eventPublisher.publishEvent(
		// 	MemberJoinedWorkspaceEvent.createEvent(workspaceMember)
		// );

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
		Invitation invitation = invitationFinder.findPendingInvitation(invitationId);
		String workspaceCode = invitation.getWorkspaceKey();

		invitationValidator.validateInvitation(memberId, workspaceCode);

		return invitation;
	}
}
