package com.uranus.taskmanager.api.workspacemember.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.common.exception.CommonException;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.repository.InvitationRepository;
import com.uranus.taskmanager.api.invitation.exception.InvitationAlreadyExistsException;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspacemember.exception.AlreadyJoinedWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.FailedInvitedMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InvitedMember;
import com.uranus.taskmanager.api.workspacemember.validator.WorkspaceMemberValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberInviteService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final InvitationRepository invitationRepository;

	private final WorkspaceMemberValidator workspaceMemberValidator;

	@Transactional
	public InviteMemberResponse inviteMember(String code, InviteMemberRequest request) {

		Workspace workspace = workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);

		Member invitedMember = memberRepository.findByMemberIdentifier(request.getMemberIdentifier())
			.orElseThrow(MemberNotFoundException::new);

		workspaceMemberValidator.validateIfAlreadyJoined(invitedMember.getId(), code);
		checkIfPendingInvitationExists(workspace, invitedMember);

		Invitation invitation = savePendingInvitation(workspace, invitedMember);

		return InviteMemberResponse.from(invitation);
	}

	/**
	 * Todo
	 *  - InvitedMember, FailedInvitedMember가 일치하지 않는 문제 때문에 equals & hashCode를 구현했다
	 *  - 추후에 중복 멤버 또는 identifier를 거르기 위해서 Set을 사용할 예정
	 *  - InvitedMember, FailedInvitedMember로 분리하지 않고 하나의 클래스를 만들어서 통합하기
	 */
	@Transactional
	public InviteMembersResponse inviteMembers(String code, InviteMembersRequest request) {
		// Todo: 일급 컬렉션으로 리팩토링하는 것을 고려. 관련 처리 로직을 해당 일급 컬렉션 클래스에서 정의
		List<InvitedMember> invitedMembers = new ArrayList<>();
		List<FailedInvitedMember> failedInvitedMembers = new ArrayList<>();

		Workspace workspace = workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);

		for (String identifier : request.getMemberIdentifiers()) {
			try {
				Member invitedMember = memberRepository.findByMemberIdentifier(identifier)
					.orElseThrow(MemberNotFoundException::new);

				workspaceMemberValidator.validateIfAlreadyJoined(invitedMember.getId(), code);
				checkIfPendingInvitationExists(workspace, invitedMember);

				savePendingInvitation(workspace, invitedMember);
				addInvitedMember(invitedMembers, invitedMember);
			} catch (AlreadyJoinedWorkspaceException | InvitationAlreadyExistsException e) {
				String errorMessage = getErrorMessageFromException(e);
				addFailedInvitedMember(identifier, failedInvitedMembers, errorMessage);
			}
		}

		return new InviteMembersResponse(invitedMembers, failedInvitedMembers);
	}

	private void checkIfPendingInvitationExists(Workspace workspace, Member invitedMember) {
		invitationRepository.findByWorkspaceAndMember(workspace, invitedMember)
			.filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
			.ifPresent(invitation -> {
				throw new InvitationAlreadyExistsException();
			});
	}

	private Invitation savePendingInvitation(Workspace workspace, Member invitedMember) {
		Invitation invitation = Invitation.builder()
			.workspace(workspace)
			.member(invitedMember)
			.status(InvitationStatus.PENDING)
			.build();
		invitationRepository.save(invitation);
		return invitation;
	}

	private static void addInvitedMember(List<InvitedMember> invitedMembers, Member invitedMember) {
		invitedMembers.add(new InvitedMember(invitedMember.getLoginId(),
			invitedMember.getEmail()));
	}

	private static void addFailedInvitedMember(String identifier, List<FailedInvitedMember> failedInvitedMembers,
		String errorMessage) {
		failedInvitedMembers.add(new FailedInvitedMember(identifier, errorMessage));
	}

	private static String getErrorMessageFromException(Exception exception) {
		return exception instanceof CommonException ? exception.getMessage() : "Invitation failed";
	}
}
