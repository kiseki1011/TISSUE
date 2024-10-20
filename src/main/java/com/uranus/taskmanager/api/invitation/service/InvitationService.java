package com.uranus.taskmanager.api.invitation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.exception.InvalidInvitationStatusException;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
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
	public InvitationAcceptResponse acceptInvitation(LoginMemberDto loginMember, String workspaceCode) {

		Invitation invitation = getPendingInvitationBy(loginMember, workspaceCode);
		changeStatusToAccepted(invitation);
		WorkspaceMember workspaceMember = addMemberToWorkspace(invitation);

		return InvitationAcceptResponse.from(invitation, workspaceMember);
	}

	@Transactional
	public void rejectInvitation(LoginMemberDto loginMember, String workspaceCode) {

		Invitation invitation = getPendingInvitationBy(loginMember, workspaceCode);
		changeStatusToRejected(invitation);
		// Todo: InvitationRejectResponse 사용을 고려
	}

	private Invitation getPendingInvitationBy(LoginMemberDto loginMember, String workspaceCode) {
		Invitation invitation = invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode,
				loginMember.getLoginId())
			.orElseThrow(InvitationNotFoundException::new);

		validatePendingStatus(invitation);

		return invitation;
	}

	private WorkspaceMember addMemberToWorkspace(Invitation invitation) {
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(invitation.getMember(),
			invitation.getWorkspace(), WorkspaceRole.USER, invitation.getMember().getEmail());
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
