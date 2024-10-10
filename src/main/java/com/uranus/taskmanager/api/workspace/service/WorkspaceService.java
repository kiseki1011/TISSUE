package com.uranus.taskmanager.api.workspace.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Todo
 * UserWorkspaceService, AdminWorkspaceService, ReaderWorkspaceService 분리 고려
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationRepository invitationRepository;

	/**
	 * Todo
	 * 조회 로직 수정 필요
	 * get -> getWorkspaceDetail
	 */
	@Transactional(readOnly = true)
	public WorkspaceResponse get(String workspaceCode) {

		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
		return WorkspaceResponse.fromEntity(workspace);
	}

	// Todo: 로직, 가독성 리팩토링
	@Transactional
	public InviteMemberResponse inviteMember(String workspaceCode, InviteMemberRequest request,
		LoginMemberDto loginMember) {

		String identifier = request.getMemberIdentifier();

		// 워크스페이스가 존재하는지 조회
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		// 초대할 멤버 조회
		Member invitedMember = memberRepository.findByLoginIdOrEmail(identifier, identifier)
			.orElseThrow(MemberNotFoundException::new);

		// 만약 워크스페이스에 이미 참여하고 있는 멤버면 예외 발생
		// Todo: isPresent() 대신 더 좋은 API가 있는지 찾아보기. (orElseThrow() 적용)
		boolean isAlreadyMember = workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(
			invitedMember.getLoginId(),
			workspaceCode).isPresent();

		if (isAlreadyMember) {
			throw new RuntimeException(
				"Member is already participating in this Workspace"); // Todo: MemberAlreadyParticipatingException 구현
		}

		// 초대가 이미 존재하는지 확인
		// 만약 초대가 이미 PENDING 상태로 있으면 예외 발생
		// Todo: Optional은 굉장히 비싸다! 최대한 빠르게 해소하는 것을 권장한다.
		//  isPresent() 말고 orElse(), orElseGet(), orElseThrow()의 사용을 고려하자.
		Optional<Invitation> existingInvitation = invitationRepository.findByWorkspaceAndMember(workspace,
			invitedMember);

		if (existingInvitation.isPresent() && existingInvitation.get().getStatus() == InvitationStatus.PENDING) {
			throw new RuntimeException(
				"An invitation for this member is already pending."); // Todo: InvitationAlreadyExistsException 추가
		}

		// 초대 생성
		Invitation invitation = Invitation.builder()
			.workspace(workspace)
			.member(invitedMember)
			.status(InvitationStatus.PENDING)
			.build();
		invitationRepository.save(invitation);

		return InviteMemberResponse.fromEntity(invitation);
	}
}
