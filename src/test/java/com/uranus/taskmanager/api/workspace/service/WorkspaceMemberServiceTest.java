package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.AlreadyJoinedWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.exception.InvalidRoleUpdateException;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.WorkspaceJoinRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.FailedInvitedMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InvitedMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.KickWorkspaceMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateWorkspaceMemberRoleResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.WorkspaceJoinResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class WorkspaceMemberServiceTest extends ServiceIntegrationTestHelper {

	private Member member;

	@BeforeEach
	void setUp() {
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("Test Workspace", "Test Description",
			"TESTCODE", null);
		member = memberRepositoryFixture.createMember("member1", "member1@test.com", "password1234!");
		workspaceRepositoryFixture.addMemberToWorkspace(member, workspace, WorkspaceRole.COLLABORATOR);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스를 조회하면, 워크스페이스를 반환한다")
	void testGetWorkspaceDetail_Success() {
		// given
		String workspaceCode = "TESTCODE";
		LoginMember loginMember = new LoginMember(member.getId(), member.getLoginId(), member.getEmail());

		// when
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(workspaceCode, loginMember.getId());

		// then
		assertThat(response).isNotNull();
		assertThat(response.getCode()).isEqualTo(workspaceCode);
	}

	@Test
	@DisplayName("유효하지 않은 워크스페이스 코드로 워크스페이스를 조회하면, 예외가 발생한다")
	void testGetWorkspaceDetail_WorkspaceNotFoundException() {
		// given
		String invalidCode = "INVALIDCODE";
		LoginMember loginMember = new LoginMember(member.getId(), member.getLoginId(), member.getEmail());

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail(invalidCode, loginMember.getId()))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}

	@Test
	@DisplayName("초대가 성공하면 초대가 PENDING 상태로 저장된다")
	void testInviteMember_Success() {
		// given
		String workspaceCode = "TESTCODE";
		Member invitedMember = memberRepositoryFixture.createMember("member2", "member2@test.com",
			"password1234!");
		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedMember.getLoginId());

		// when
		InviteMemberResponse inviteMemberResponse = workspaceMemberService.inviteMember(workspaceCode,
			inviteMemberRequest);

		// then
		assertThat(inviteMemberResponse.getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	@DisplayName("존재하지 않는 멤버를 초대하면 예외가 발생한다")
	void testInviteMember_MemberNotFoundException() {
		// given
		String workspaceCode = "TESTCODE";
		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest("nonexistentUser");

		// when & then
		assertThatThrownBy(() -> workspaceMemberService.inviteMember(workspaceCode, inviteMemberRequest))
			.isInstanceOf(MemberNotFoundException.class);
	}

	@Test
	@DisplayName("해당 워크스페이스에 이미 참여하고 있는 멤버를 다시 초대하면 예외가 발생한다")
	void testInviteMember_AlreadyJoinedWorkspaceException() {
		// given
		String workspaceCode = "TESTCODE";
		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(member.getLoginId());

		// when & then
		assertThatThrownBy(() -> workspaceMemberService.inviteMember(workspaceCode, inviteMemberRequest))
			.isInstanceOf(AlreadyJoinedWorkspaceException.class);
	}

	/**
	 * Todo
	 *  - InvitedMember, FailedInvitedMember가 일치하지 않는 문제 때문에 equals & hashCode를 구현했다
	 *  - 추후에 중복 멤버 또는 identifier를 거르기 위해서 Set을 사용할 예정
	 *  - InvitedMember, FailedInvitedMember로 분리하지 않고 하나의 클래스를 만들어서 통합하기
	 */
	@Test
	@DisplayName("다수의 멤버의 초대를 시도하면, 성공한 멤버와 실패한 멤버로 나뉜 응답을 받을 수 있다")
	void testInviteMembers_Success() {
		// given
		String workspaceCode = "TESTCODE";
		Member member2 = memberRepositoryFixture.createMember("member2", "member2@test.com", "password1234!");
		Member member3 = memberRepositoryFixture.createMember("member3", "member3@test.com", "password1234!");

		InviteMembersRequest request = new InviteMembersRequest(
			List.of(member.getLoginId(), member2.getLoginId(), member3.getLoginId()));

		List<InvitedMember> invitedMembers = List.of(new InvitedMember(member2.getLoginId(), member2.getEmail()),
			new InvitedMember(member3.getLoginId(), member3.getEmail()));
		List<FailedInvitedMember> failedInvitedMembers = List.of(
			new FailedInvitedMember("member1", "Member already joined this Workspace"));

		// when
		InviteMembersResponse response = workspaceMemberService.inviteMembers(workspaceCode, request);

		// then
		assertThat(response.getInvitedMembers()).isEqualTo(invitedMembers);
		assertThat(response.getFailedInvitedMembers()).isEqualTo(failedInvitedMembers);
	}

	@Test
	@DisplayName("워크스페이스 참여 시 비밀번호가 일치하지 않는 경우 예외가 발생한다")
	void testJoinWorkspace_InvalidPasswordException() {
		// given
		String workspaceCode = "CODE1234";
		workspaceRepositoryFixture.createWorkspace("Workspace", "Description",
			"CODE1234", "password1234!");
		WorkspaceJoinRequest request = new WorkspaceJoinRequest("WrongPassword1234!");
		LoginMember loginMember = new LoginMember(member.getId(), member.getLoginId(), member.getEmail());

		// when & then
		assertThatThrownBy(() -> workspaceMemberService.joinWorkspace(workspaceCode, request, loginMember.getId()))
			.isInstanceOf(InvalidWorkspacePasswordException.class);
	}

	@Test
	@DisplayName("워크스페이스 참여가 성공하는 경우 워크스페이스 참여 응답을 정상적으로 반환한다")
	void testJoinWorkspace_Success() {
		// given
		String workspaceCode = "TESTCODE";
		WorkspaceJoinRequest request = new WorkspaceJoinRequest(null);
		LoginMember loginMember = new LoginMember(member.getId(), member.getLoginId(), member.getEmail());

		// when
		WorkspaceJoinResponse response = workspaceMemberService.joinWorkspace(workspaceCode, request,
			loginMember.getId());

		// then
		assertThat(response).isNotNull();
	}

	@Test
	@DisplayName("이미 워크스페이스에 참여하는 멤버가 참여를 시도하는 경우 응답은 정상적으로 반환되나, 참여 플래그를 true로 반환받는다")
	void testJoinWorkspace_isAlreadyMemberTrue() {
		// given
		String workspaceCode = "TESTCODE";
		WorkspaceJoinRequest request = new WorkspaceJoinRequest(null);
		LoginMember loginMember = new LoginMember(member.getId(), member.getLoginId(), member.getEmail());

		// when
		WorkspaceJoinResponse response = workspaceMemberService.joinWorkspace(workspaceCode, request,
			loginMember.getId());

		// then
		assertThat(response).isNotNull();
		assertThat(response.isAlreadyMember()).isTrue();
	}

	@Test
	@DisplayName("해당 워크스페이스에 참여하지 않은 멤버가 참여에 성공하는 경우, 참여 플래그를 false로 반환받는다")
	void testJoinWorkspace_isAlreadyMemberFalse() {
		// given
		String workspaceCode = "TESTCODE";
		WorkspaceJoinRequest request = new WorkspaceJoinRequest(null);
		Member joiningMember = memberRepositoryFixture.createMember("member2", "member2@test.com",
			"password1234!");
		LoginMember loginMember = new LoginMember(joiningMember.getId(), joiningMember.getLoginId(),
			joiningMember.getEmail());

		// when
		WorkspaceJoinResponse response = workspaceMemberService.joinWorkspace(workspaceCode, request,
			loginMember.getId());

		// then
		assertThat(response).isNotNull();
		assertThat(response.isAlreadyMember()).isFalse();
	}

	/**
	 * 트랜잭션 애노테이션 제거 시 LazyInitializationException 발생
	 * <p>
	 * 원인:
	 * - Hibernate 세션이 닫힌 상태에서 지연 로딩된 속성에 접근하려 할 때 발생합니다.
	 * - 현재 테스트의 경우, Workspace 엔티티의 workspaceMembers 컬렉션을 액세스하려고 할 때
	 * 세션이 종료되어 이 오류가 발생합니다.
	 */
	@Transactional
	@Test
	@DisplayName("멤버가 워크스페이스에서 성공적으로 추방되면 응답이 반환되어야 한다")
	void kickWorkspaceMember_Success() {
		// given
		String workspaceCode = "TESTCODE";

		Member member2 = memberRepositoryFixture.createMember("member2", "member2@test.com", "password1234!");
		workspaceMemberService.joinWorkspace(workspaceCode, new WorkspaceJoinRequest(), member2.getId());

		KickWorkspaceMemberRequest request = new KickWorkspaceMemberRequest(member2.getLoginId());

		// when
		KickWorkspaceMemberResponse response = workspaceMemberService.kickWorkspaceMember(workspaceCode, request);

		// then
		assertThat(member2.getLoginId()).isEqualTo(response.getMemberIdentifier());
		assertThat(
			workspaceMemberRepository.findByMemberIdAndWorkspaceCode(member2.getId(), workspaceCode)).isPresent();
	}

	@Test
	@DisplayName("존재하지 않는 멤버를 추방하려고 하면 예외가 발생한다")
	void kickWorkspaceMember_MemberNotFoundException() {
		// given
		String workspaceCode = "TESTCODE";
		KickWorkspaceMemberRequest request = new KickWorkspaceMemberRequest("nonExistentIdentifier");

		// when & then
		assertThatThrownBy(() -> workspaceMemberService.kickWorkspaceMember(workspaceCode, request))
			.isInstanceOf(MemberNotFoundException.class);
	}

	@Test
	@DisplayName("워크스페이스에 소속되지 않은 멤버를 추방하려는 경우 예외가 발생 한다")
	void kickWorkspaceMember_MemberNotInWorkspaceException() {
		// given
		String workspaceCode = "TESTCODE";
		Member nonWorkspaceMember = memberRepositoryFixture.createMember("member3", "member3@test.com",
			"password1234!");

		KickWorkspaceMemberRequest request = new KickWorkspaceMemberRequest(nonWorkspaceMember.getLoginId());

		// when & then
		assertThatThrownBy(() -> workspaceMemberService.kickWorkspaceMember(workspaceCode, request))
			.isInstanceOf(MemberNotInWorkspaceException.class);
	}

	@Transactional
	@Test
	@DisplayName("특정 워크스페이스의 멤버의 권한을 변경에 성공하면 변경된 멤버의 정보를 응답으로 반환한다")
	void updateWorkspaceMember_success_returnsUpdateWorkspaceMemberRoleResponse() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE").get();

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(requester, workspace,
			WorkspaceRole.MANAGER, requester.getEmail());
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(targetMember, workspace,
			WorkspaceRole.VIEWER, targetMember.getEmail());
		workspaceMemberRepository.save(targetWorkspaceMember);

		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest("target",
			WorkspaceRole.MANAGER);

		// when
		UpdateWorkspaceMemberRoleResponse response = workspaceMemberService.updateWorkspaceMemberRole("TESTCODE",
			request, requester.getId());

		// then
		assertThat(response.getWorkspaceMemberDetail().getNickname()).isEqualTo("target123@test.com");
		assertThat(response.getWorkspaceMemberDetail().getWorkspaceRole()).isEqualTo(WorkspaceRole.MANAGER);
	}

	@Transactional
	@Test
	@DisplayName("자기 자신의 워크스페이스 멤버 권한을 변경하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateItself() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE").get();

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(requester, workspace,
			WorkspaceRole.MANAGER, requester.getEmail());
		workspaceMemberRepository.save(requesterWorkspaceMember);

		// when & then
		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest("requester",
			WorkspaceRole.MANAGER);

		assertThatThrownBy(() -> workspaceMemberService.updateWorkspaceMemberRole("TESTCODE",
			request, requester.getId())).isInstanceOf(InvalidRoleUpdateException.class);
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 멤버의 권한을 OWNER로 변경하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateToOwner() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE").get();

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(requester, workspace,
			WorkspaceRole.MANAGER, requester.getEmail());
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(targetMember, workspace,
			WorkspaceRole.VIEWER, targetMember.getEmail());
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest("target",
			WorkspaceRole.OWNER);

		assertThatThrownBy(() -> workspaceMemberService.updateWorkspaceMemberRole("TESTCODE",
			request, requester.getId())).isInstanceOf(InvalidRoleUpdateException.class);
	}

	@Transactional
	@Test
	@DisplayName("자기 자신보다 높은 권한을 가진 멤버를 업데이트하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateHigherRoleWorkspaceMember() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE").get();

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(requester, workspace,
			WorkspaceRole.MANAGER, requester.getEmail());
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(targetMember, workspace,
			WorkspaceRole.OWNER, targetMember.getEmail());
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest("target",
			WorkspaceRole.MANAGER);

		assertThatThrownBy(() -> workspaceMemberService.updateWorkspaceMemberRole("TESTCODE",
			request, requester.getId())).isInstanceOf(InvalidRoleUpdateException.class);
	}

	@Transactional
	@Test
	@DisplayName("OWNER 권한을 가진 멤버가 다른 멤버를 OWNER 권한으로 업데이트하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifOwnerTrysToUpdateWorkspaceMemberToOwner() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE").get();

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(requester, workspace,
			WorkspaceRole.OWNER, requester.getEmail());
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(targetMember, workspace,
			WorkspaceRole.MANAGER, targetMember.getEmail());
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateWorkspaceMemberRoleRequest request = new UpdateWorkspaceMemberRoleRequest("target",
			WorkspaceRole.OWNER);

		assertThatThrownBy(() -> workspaceMemberService.updateWorkspaceMemberRole("TESTCODE",
			request, requester.getId())).isInstanceOf(InvalidRoleUpdateException.class);
	}
}
