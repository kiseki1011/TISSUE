package com.uranus.taskmanager.api.workspacemember.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.InvalidRoleUpdateException;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.RemoveMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.TransferOwnershipRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.RemoveMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateMemberRoleResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class WorkspaceMemberCommandServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
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
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepositoryFixture.createMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);
		WorkspaceMember.addOwnerWorkspaceMember(requester, workspace);

		Member target = memberRepositoryFixture.createMember(
			"member2",
			"member2@test.com",
			"password1234!"
		);
		workspaceRepositoryFixture.addMemberToWorkspace(target, workspace, WorkspaceRole.COLLABORATOR);

		RemoveMemberRequest request = new RemoveMemberRequest(target.getLoginId());

		// when
		RemoveMemberResponse response = workspaceMemberCommandService.kickOutMember("TESTCODE", request,
			requester.getId());

		// then
		assertThat(response.getMemberIdentifier()).isEqualTo(target.getLoginId());
		assertThat(
			workspaceMemberRepository.findByMemberIdAndWorkspaceCode(target.getId(), "TESTCODE")
		).isEmpty();
	}

	@Test
	@DisplayName("존재하지 않는 멤버를 추방하려고 하면 예외가 발생한다")
	void kickWorkspaceMember_MemberNotFoundException() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepositoryFixture.createMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		workspaceRepositoryFixture.addMemberToWorkspace(requester, workspace, WorkspaceRole.OWNER);

		RemoveMemberRequest request = new RemoveMemberRequest("nonExistentIdentifier");

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.kickOutMember("TESTCODE", request, requester.getId()))
			.isInstanceOf(MemberNotFoundException.class);
	}

	@Test
	@DisplayName("워크스페이스에 소속되지 않은 멤버를 추방하려는 경우 예외가 발생 한다")
	void kickWorkspaceMember_MemberNotInWorkspaceException() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepositoryFixture.createMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		workspaceRepositoryFixture.addMemberToWorkspace(requester, workspace, WorkspaceRole.OWNER);

		Member nonWorkspaceMember = memberRepositoryFixture.createMember(
			"notJoinedMember",
			"notJoinedMember@test.com",
			"password1234!");

		RemoveMemberRequest request = new RemoveMemberRequest(nonWorkspaceMember.getLoginId());

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.kickOutMember("TESTCODE", request, requester.getId()))
			.isInstanceOf(MemberNotInWorkspaceException.class);
	}

	@Transactional
	@Test
	@DisplayName("특정 워크스페이스의 멤버의 권한을 변경에 성공하면 변경된 멤버의 정보를 응답으로 반환한다")
	void updateWorkspaceMember_success_returnsUpdateWorkspaceMemberRoleResponse() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			requester,
			workspace,
			WorkspaceRole.MANAGER,
			requester.getEmail()
		);
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.VIEWER,
			targetMember.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest("target",
			WorkspaceRole.MANAGER);

		// when
		UpdateMemberRoleResponse response = workspaceMemberCommandService.updateWorkspaceMemberRole("TESTCODE",
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
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			requester,
			workspace,
			WorkspaceRole.MANAGER,
			requester.getEmail()
		);
		workspaceMemberRepository.save(requesterWorkspaceMember);

		// when & then
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest("requester",
			WorkspaceRole.MANAGER);

		assertThatThrownBy(() -> workspaceMemberCommandService.updateWorkspaceMemberRole("TESTCODE",
			request, requester.getId())).isInstanceOf(InvalidRoleUpdateException.class);
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 멤버의 권한을 OWNER로 변경하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateToOwner() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			requester,
			workspace,
			WorkspaceRole.MANAGER,
			requester.getEmail()
		);
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.VIEWER,
			targetMember.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest("target",
			WorkspaceRole.OWNER);

		assertThatThrownBy(() ->
			workspaceMemberCommandService.updateWorkspaceMemberRole("TESTCODE", request, requester.getId()))
			.isInstanceOf(InvalidRoleUpdateException.class);
	}

	@Transactional
	@Test
	@DisplayName("자기 자신보다 높은 권한을 가진 멤버를 업데이트하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateHigherRoleWorkspaceMember() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			requester,
			workspace,
			WorkspaceRole.MANAGER,
			requester.getEmail()
		);
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.OWNER,
			targetMember.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest("target",
			WorkspaceRole.MANAGER);

		assertThatThrownBy(() ->
			workspaceMemberCommandService.updateWorkspaceMemberRole("TESTCODE", request, requester.getId()))
			.isInstanceOf(InvalidRoleUpdateException.class);
	}

	@Transactional
	@Test
	@DisplayName("OWNER 권한을 가진 멤버가 다른 멤버를 OWNER 권한으로 업데이트하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifOwnerTrysToUpdateWorkspaceMemberToOwner() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			requester,
			workspace,
			WorkspaceRole.OWNER,
			requester.getEmail()
		);
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.MANAGER,
			targetMember.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest("target",
			WorkspaceRole.OWNER);

		assertThatThrownBy(() ->
			workspaceMemberCommandService.updateWorkspaceMemberRole("TESTCODE", request, requester.getId()))
			.isInstanceOf(InvalidRoleUpdateException.class);
	}

	@Transactional
	@Test
	@DisplayName("OWNER 권한을 가진 멤버가 다른 멤버로 워크스페이스 소유권 이전을 성공하면 응답이 반환된다")
	void testTransferWorkspaceOwnership_ifSuccess_returnTransferWorkspaceOwnershipResponse() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requester = memberRepository.save(Member.builder()
			.loginId("requester")
			.email("requester123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember requesterWorkspaceMember = WorkspaceMember.addOwnerWorkspaceMember(
			requester,
			workspace
		);
		workspaceMemberRepository.save(requesterWorkspaceMember);

		Member targetMember = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.MANAGER,
			targetMember.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		TransferOwnershipRequest request = new TransferOwnershipRequest("target");

		// when
		TransferOwnershipResponse response = workspaceMemberCommandService.transferWorkspaceOwnership(
			"TESTCODE",
			request,
			requester.getId()
		);

		// then
		assertThat(response).isNotNull();
	}
}
