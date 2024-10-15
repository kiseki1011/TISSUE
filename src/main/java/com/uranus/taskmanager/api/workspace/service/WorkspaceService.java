package com.uranus.taskmanager.api.workspace.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.common.CommonException;
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
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
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
 * UserWorkspaceService, AdminWorkspaceService, ReaderWorkspaceService 분리 고려
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final InvitationRepository invitationRepository;
	private final PasswordEncoder passwordEncoder;

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
			throw new MemberAlreadyParticipatingException();
		}

		// 초대가 이미 존재하는지 확인
		// 만약 초대가 이미 PENDING 상태로 있으면 예외 발생
		// Todo: Optional은 굉장히 비싸다! 최대한 빠르게 해소하는 것을 권장한다.
		//  isPresent() 말고 orElse(), orElseGet(), orElseThrow()의 사용을 고려하자.
		Optional<Invitation> existingInvitation = invitationRepository.findByWorkspaceAndMember(workspace,
			invitedMember);

		if (existingInvitation.isPresent() && existingInvitation.get().getStatus() == InvitationStatus.PENDING) {
			throw new InvitationAlreadyExistsException();
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

	@Transactional
	public InviteMembersResponse inviteMembers(String workspaceCode, InviteMembersRequest request,
		LoginMemberDto loginMember) {
		// Todo: 일급 컬렉션으로 리팩토링하는 것을 고려. 관련 처리 로직을 해당 일급 컬렉션 클래스에서 정의.
		List<InvitedMember> invitedMembers = new ArrayList<>();
		List<FailedInvitedMember> failedInvitedMembers = new ArrayList<>();

		// 워크스페이스가 존재하는지 조회
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		// identifier들의 목록을 순회하면서 초대 처리 로직 수행
		for (String identifier : request.getMemberIdentifiers()) {
			try {
				// 초대할 멤버 조회
				Member invitedMember = memberRepository.findByLoginIdOrEmail(identifier, identifier)
					.orElseThrow(MemberNotFoundException::new);

				// 만약 워크스페이스에 이미 참여하고 있는 멤버면 예외 발생
				boolean isAlreadyMember = workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(
					invitedMember.getLoginId(),
					workspaceCode).isPresent();

				if (isAlreadyMember) {
					throw new MemberAlreadyParticipatingException();
				}

				// 초대의 존재 확인, 만약 초대가 이미 PENDING 상태로 있으면 예외 발생
				Optional<Invitation> existingInvitation = invitationRepository.findByWorkspaceAndMember(workspace,
					invitedMember);

				if (existingInvitation.isPresent()
					&& existingInvitation.get().getStatus() == InvitationStatus.PENDING) {
					throw new InvitationAlreadyExistsException();
				}

				// 초대 생성 로직
				Invitation invitation = Invitation.builder()
					.workspace(workspace)
					.member(invitedMember)
					.status(InvitationStatus.PENDING)
					.build();
				invitationRepository.save(invitation);

				// 성공한 초대 추가
				invitedMembers.add(new InvitedMember(invitedMember.getLoginId(),
					invitedMember.getEmail())); // Todo: InvitedMember에서 identifier만 사용하는 것 고려

				// Todo: 현재 위에서 발생한 예외를 바로 catch로 잡는 형태이다.
				//  예외는 굉장히 비싸다는 것을 알아두자! 예외를 발생시키지 않고 처리하는 방법을 찾아보자.
			} catch (Exception e) {
				// 실패한 경우 오류 메시지에 따라 추가
				// Todo: 리팩토링 고민
				String errorMessage = e instanceof CommonException ? e.getMessage() : "Invitation failed";
				failedInvitedMembers.add(new FailedInvitedMember(identifier, errorMessage));
			}
		}

		return new InviteMembersResponse(invitedMembers, failedInvitedMembers);
	}

	@Transactional
	public WorkspaceParticipateResponse participateWorkspace(String workspaceCode, WorkspaceParticipateRequest request,
		LoginMemberDto loginMember) {

		// 워크스페이스가 존재하는지 조회
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		// 로그인 정보에서 가져온 정보를 통해 멤버 조회
		Member member = memberRepository.findByLoginId(loginMember.getLoginId())
			.orElseThrow(MemberNotFoundException::new);

		// 워크스페이스 참여 인원 계산
		// Todo: WorkspaceParticipateResponse 참고, Workspace 엔티티에 캐싱 필드를 추가하는 것을 고려
		int headcount = workspaceMemberRepository.countByWorkspaceId(workspace.getId());

		// 만약 워크스페이스에 이미 참여하고 있는 멤버면 예외 발생
		// 예외 방식 -> Early return 방식으로 변경
		Optional<WorkspaceMember> findWorkspaceMember = workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(
			loginMember.getLoginId(),
			workspaceCode);

		boolean isAlreadyMember = findWorkspaceMember.isPresent();

		if (isAlreadyMember) {
			return WorkspaceParticipateResponse.from(workspace, findWorkspaceMember.get(), headcount, isAlreadyMember);
		}

		// 만약 워크스페이스가 비밀번호를 가지고 있고, 비밀번호 대조를 실패하면 예외 발생
		// Todo: null을 사용하지 않고 Optional 사용
		if (workspace.getPassword() != null) {
			if (!passwordEncoder.matches(request.getPassword(), workspace.getPassword())) {
				throw new InvalidWorkspacePasswordException();
			}
		}

		// 워크스페이스 참여
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.USER,
			member.getEmail());

		return WorkspaceParticipateResponse.from(workspace, workspaceMember, headcount, isAlreadyMember);
	}
}
