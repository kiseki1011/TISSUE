package com.uranus.taskmanager.api.workspacemember.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.exception.DuplicateNicknameException;
import com.uranus.taskmanager.api.workspacemember.exception.InvalidRoleUpdateException;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateMemberNicknameRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.RemoveMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateMemberNicknameResponse;
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

		// when
		RemoveMemberResponse response = workspaceMemberCommandService.removeMember("TESTCODE", target.getId(),
			requester.getId());

		// then
		assertThat(response.getMemberId()).isEqualTo(target.getId());
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

		Long nonExistMemberId = 999L;

		// when & then
		assertThatThrownBy(
			() -> workspaceMemberCommandService.removeMember("TESTCODE", nonExistMemberId, requester.getId()))
			.isInstanceOf(MemberNotInWorkspaceException.class);
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

		// when & then
		assertThatThrownBy(
			() -> workspaceMemberCommandService.removeMember("TESTCODE", nonWorkspaceMember.getId(), requester.getId()))
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

		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(WorkspaceRole.MANAGER);

		// when
		UpdateMemberRoleResponse response = workspaceMemberCommandService.updateWorkspaceMemberRole(
			"TESTCODE",
			targetMember.getId(),
			requester.getId(),
			request
		);

		// then
		assertThat(response.getTargetDetail().getNickname()).isEqualTo("target123@test.com");
		assertThat(response.getTargetDetail().getWorkspaceRole()).isEqualTo(WorkspaceRole.MANAGER);
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
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(WorkspaceRole.MANAGER);

		assertThatThrownBy(() -> workspaceMemberCommandService.updateWorkspaceMemberRole(
			"TESTCODE",
			requester.getId(),
			requester.getId(),
			request
		))
			.isInstanceOf(InvalidRoleUpdateException.class);
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

		Member target = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			target,
			workspace,
			WorkspaceRole.VIEWER,
			target.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(WorkspaceRole.OWNER);

		assertThatThrownBy(() ->
			workspaceMemberCommandService.updateWorkspaceMemberRole(
				"TESTCODE",
				target.getId(),
				requester.getId(),
				request
			))
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

		Member target = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			target,
			workspace,
			WorkspaceRole.OWNER,
			target.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(WorkspaceRole.MANAGER);

		assertThatThrownBy(() ->
			workspaceMemberCommandService.updateWorkspaceMemberRole(
				"TESTCODE",
				target.getId(),
				requester.getId(),
				request
			))
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

		Member target = memberRepository.save(Member.builder()
			.loginId("target")
			.email("target123@test.com")
			.password("password1234!")
			.build());

		WorkspaceMember targetWorkspaceMember = WorkspaceMember.addWorkspaceMember(
			target,
			workspace,
			WorkspaceRole.MANAGER,
			target.getEmail()
		);
		workspaceMemberRepository.save(targetWorkspaceMember);

		// when & then
		UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(WorkspaceRole.OWNER);

		assertThatThrownBy(() ->
			workspaceMemberCommandService.updateWorkspaceMemberRole(
				"TESTCODE",
				target.getId(),
				requester.getId(),
				request
			))
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
			.build()
		);

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

		WorkspaceMember target = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.MANAGER,
			targetMember.getEmail()
		);
		workspaceMemberRepository.save(target);

		// when
		TransferOwnershipResponse response = workspaceMemberCommandService.transferWorkspaceOwnership(
			"TESTCODE",
			target.getId(),
			requester.getId()
		);

		// then
		assertThat(response).isNotNull();
	}

	@Test
	@DisplayName("워크스페이스 멤버의 별칭을 성공적으로 변경하면 응답으로 상세 정보를 반환한다")
	void updateNickname_Success() {
		// given
		Member tester = memberRepository.save(Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("password1234!")
			.build()
		);

		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		workspaceMemberRepository.save(
			WorkspaceMember.addCollaboratorWorkspaceMember(tester, workspace)
		);

		String newNickname = "newNickname";
		UpdateMemberNicknameRequest request = new UpdateMemberNicknameRequest(newNickname);

		// when
		UpdateMemberNicknameResponse response = workspaceMemberCommandService.updateWorkspaceMemberNickname(
			workspace.getCode(),
			tester.getId(),
			request
		);

		// then
		assertThat(response.getUpdatedDetail().getNickname()).isEqualTo(newNickname);

		WorkspaceMember updatedMember = workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(tester.getId(), workspace.getCode())
			.orElseThrow();
		assertThat(updatedMember.getNickname()).isEqualTo(newNickname);
	}

	@Test
	@DisplayName("이미 사용 중인 별칭으로 변경 시 예외가 발생한다")
	void updateNickname_Failed_DuplicateNickname() {
		// given
		Member tester = memberRepository.save(Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("password1234!")
			.build()
		);
		Member existingMember = memberRepository.save(Member.builder()
			.loginId("existingMember")
			.email("existingMember@test.com")
			.password("password1234!")
			.build()
		);

		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		workspaceMemberRepository.save(
			WorkspaceMember.addCollaboratorWorkspaceMember(tester, workspace)
		);
		workspaceMemberRepository.save(
			WorkspaceMember.addWorkspaceMember(
				existingMember,
				workspace,
				WorkspaceRole.COLLABORATOR,
				"existingNickname"
			)
		);

		String existingNickname = "existingNickname";
		UpdateMemberNicknameRequest request = new UpdateMemberNicknameRequest(existingNickname);

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.updateWorkspaceMemberNickname(
			workspace.getCode(),
			tester.getId(),
			request
		)).isInstanceOfAny(DuplicateNicknameException.class);

	}

}
