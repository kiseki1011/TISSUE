package com.uranus.taskmanager.api.invitation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.exception.InvalidInvitationStatusException;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService {

	private final InvitationRepository invitationRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional
	public InvitationAcceptResponse acceptInvitation(Long loginMemberId, String workspaceCode) {

		Invitation invitation = getPendingInvitationBy(loginMemberId, workspaceCode);
		changeStatusToAccepted(invitation);
		addMemberToWorkspace(invitation);

		return InvitationAcceptResponse.from(invitation.getWorkspace(), WorkspaceRole.USER);
	}

	@Transactional
	public void rejectInvitation(Long loginMemberId, String workspaceCode) {

		Invitation invitation = getPendingInvitationBy(loginMemberId, workspaceCode);
		changeStatusToRejected(invitation);
		// Todo: InvitationRejectResponse 사용을 고려
	}

	private Invitation getPendingInvitationBy(Long loginMemberId, String workspaceCode) {
		Invitation invitation = invitationRepository.findByWorkspaceCodeAndMemberId(workspaceCode,
				loginMemberId)
			.orElseThrow(InvitationNotFoundException::new);

		validatePendingStatus(invitation);

		return invitation;
	}

	/*
	 * Todo
	 *  - 워크스페이스에 낙관적락 적용 시 예외 잡고 재시도 로직 추가 필요
	 *  - 이 메서드에 try-catch vs acceptInvitation에서 try-catch를 할지 고민
	 */
	private WorkspaceMember addMemberToWorkspace(Invitation invitation) {
		Workspace workspace = invitation.getWorkspace();
		Member member = invitation.getMember();
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(member,
			workspace, WorkspaceRole.USER, member.getEmail());

		workspace.increaseMemberCount();

		workspaceMemberRepository.save(workspaceMember);

		return workspaceMember;
	}

	private void changeStatusToAccepted(Invitation invitation) {
		invitation.changeStatus(InvitationStatus.ACCEPTED);
	}

	private void changeStatusToRejected(Invitation invitation) {
		invitation.changeStatus(InvitationStatus.REJECTED);
	}

	private void validatePendingStatus(Invitation invitation) {
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new InvalidInvitationStatusException();
		}
	}

}
