package com.uranus.taskmanager.api.workspace.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.common.exception.CommonException;
import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.exception.InvitationAlreadyExistsException;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceParticipateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.FailedInvitedMember;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InvitedMember;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceParticipateResponse;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.MemberAlreadyParticipatingException;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Todo
 *  - UserWorkspaceService, AdminWorkspaceService 분리 고려
 *  - Command용 서비스, Query용 서비스 분리 고려
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationRepository invitationRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public InviteMemberResponse inviteMember(String workspaceCode, InviteMemberRequest request,
		LoginMemberDto loginMember) { // Todo: loginMemberDto를 굳이 사용하지 않는다. 리팩토링 고려.

		Workspace workspace = findWorkspaceByCode(workspaceCode);
		Member invitedMember = findMemberByIdentifier(request.getMemberIdentifier());

		checkIfMemberAlreadyParticipates(workspaceCode, invitedMember);
		checkIfPendingInvitationExists(workspace, invitedMember);

		Invitation invitation = savePendingInvitation(workspace, invitedMember);

		return InviteMemberResponse.from(invitation);
	}

	@Transactional
	public InviteMembersResponse inviteMembers(String workspaceCode, InviteMembersRequest request,
		LoginMemberDto loginMember) {
		// Todo: 일급 컬렉션으로 리팩토링하는 것을 고려. 관련 처리 로직을 해당 일급 컬렉션 클래스에서 정의.
		//  - loginMemberDto를 굳이 사용하지 않는다. 리팩토링 고려.
		List<InvitedMember> invitedMembers = new ArrayList<>();
		List<FailedInvitedMember> failedInvitedMembers = new ArrayList<>();

		Workspace workspace = findWorkspaceByCode(workspaceCode);

		for (String identifier : request.getMemberIdentifiers()) {
			try {
				Member invitedMember = findMemberByIdentifier(identifier);

				checkIfMemberAlreadyParticipates(workspaceCode, invitedMember);
				checkIfPendingInvitationExists(workspace, invitedMember);

				savePendingInvitation(workspace, invitedMember);
				addInvitedMember(invitedMembers, invitedMember);
			} catch (Exception e) {
				String errorMessage = getErrorMessageFromException(e);
				addFailedInvitedMember(identifier, failedInvitedMembers, errorMessage);
			}
		}

		return new InviteMembersResponse(invitedMembers, failedInvitedMembers);
	}

	@Transactional
	public WorkspaceParticipateResponse participateWorkspace(String workspaceCode, WorkspaceParticipateRequest request,
		LoginMemberDto loginMember) {

		Workspace workspace = findWorkspaceByCode(workspaceCode);
		Member member = findMemberByLoginId(loginMember);

		// Todo: WorkspaceParticipateResponse 참고, Workspace 엔티티에 인원에 대한 캐싱 필드를 추가하는 것을 고려
		int headcount = getHeadcount(workspace);

		Optional<WorkspaceMember> optionalWorkspaceMember = findExistingWorkspaceMember(
			workspaceCode, loginMember);
		if (optionalWorkspaceMember.isPresent()) {
			return WorkspaceParticipateResponse.from(workspace, optionalWorkspaceMember.get(), headcount,
				true);
		}

		validatePasswordIfExists(workspace.getPassword(), request.getPassword());

		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.USER,
			member.getEmail());
		workspaceMemberRepository.save(workspaceMember);

		return WorkspaceParticipateResponse.from(workspace, workspaceMember, headcount, false);
	}

	private Workspace findWorkspaceByCode(String workspaceCode) {
		return workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
	}

	private Member findMemberByIdentifier(String identifier) {
		return memberRepository.findByLoginIdOrEmail(identifier, identifier)
			.orElseThrow(MemberNotFoundException::new);
	}

	private Member findMemberByLoginId(LoginMemberDto loginMember) {
		return memberRepository.findByLoginId(loginMember.getLoginId())
			.orElseThrow(MemberNotFoundException::new);
	}

	private void checkIfMemberAlreadyParticipates(String workspaceCode, Member invitedMember) {
		boolean isAlreadyMember = workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(
			invitedMember.getLoginId(),
			workspaceCode).isPresent();

		if (isAlreadyMember) {
			throw new MemberAlreadyParticipatingException();
		}
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

	private void validatePasswordIfExists(String workspacePassword, String inputPassword) {
		if (workspacePassword == null) {
			return;
		}
		if (passwordDoesNotMatch(workspacePassword, inputPassword)) {
			throw new InvalidWorkspacePasswordException();
		}
	}

	private boolean passwordDoesNotMatch(String workspacePassword, String inputPassword) {
		return !passwordEncoder.matches(inputPassword, workspacePassword);
	}

	private Optional<WorkspaceMember> findExistingWorkspaceMember(String workspaceCode, LoginMemberDto loginMember) {
		return workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(
			loginMember.getLoginId(),
			workspaceCode);
	}

	private int getHeadcount(Workspace workspace) {
		return workspaceMemberRepository.countByWorkspaceId(workspace.getId());
	}
}
