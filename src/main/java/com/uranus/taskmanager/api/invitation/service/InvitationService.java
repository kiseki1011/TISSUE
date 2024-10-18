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

	/**
	 * Todo: 로직과 가독성 리팩토링
	 */
	@Transactional
	public InvitationAcceptResponse acceptInvitation(LoginMemberDto loginMember, String workspaceCode) {
		// 해당 워크스페이스와 로그인된 멤버에 대한 초대 조회
		Invitation invitation = invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode,
				loginMember.getLoginId())
			.orElseThrow(InvitationNotFoundException::new);

		// PENDING 상태가 아니라면 예외 발생
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new InvalidInvitationStatusException();
		}

		// 초대 수락
		invitation.changeStatus(InvitationStatus.ACCEPTED);
		invitationRepository.save(invitation);

		// 초대를 수락한 멤버를 워크스페이스에 참여시키기
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(invitation.getMember(),
			invitation.getWorkspace(), WorkspaceRole.USER, invitation.getMember().getEmail());
		workspaceMemberRepository.save(workspaceMember);

		return InvitationAcceptResponse.from(invitation, workspaceMember);
	}

	@Transactional
	public void rejectInvitation(LoginMemberDto loginMember, String workspaceCode) {
		// 해당 워크스페이스와 로그인된 멤버에 대한 초대 조회
		Invitation invitation = invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode,
				loginMember.getLoginId())
			.orElseThrow(InvitationNotFoundException::new);

		// PENDING 상태가 아니라면 예외 발생
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new InvalidInvitationStatusException();
		}

		// 초대 거절
		invitation.changeStatus(InvitationStatus.REJECTED);
		invitationRepository.save(invitation);
	}

}
