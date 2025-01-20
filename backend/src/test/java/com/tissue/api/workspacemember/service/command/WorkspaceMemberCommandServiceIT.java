package com.tissue.api.workspacemember.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.position.domain.Position;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateNicknameRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.tissue.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateNicknameResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateRoleResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

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
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		WorkspaceMember requester = WorkspaceMember.addOwnerWorkspaceMember(requesterMember, workspace);

		Member targetMember = memberRepositoryFixture.createAndSaveMember(
			"member2",
			"member2@test.com",
			"password1234!"
		);

		WorkspaceMember target = workspaceRepositoryFixture.addAndSaveMemberToWorkspace(
			targetMember,
			workspace,
			WorkspaceRole.MEMBER
		);
		entityManager.flush();

		// when
		workspaceMemberCommandService.removeWorkspaceMember(
			target.getId(),
			requester.getId()
		);

		// then
		assertThat(workspaceMemberRepository.findById(target.getId())).isEmpty();
	}

	@Test
	@DisplayName("존재하지 않는 멤버를 추방하려고 하면 예외가 발생한다")
	void kickWorkspaceMember_MemberNotFoundException() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		WorkspaceMember requester = workspaceRepositoryFixture.addAndSaveMemberToWorkspace(
			requesterMember,
			workspace,
			WorkspaceRole.OWNER
		);

		Long nonExistMemberId = 999L;

		// when & then
		assertThatThrownBy(
			() -> workspaceMemberCommandService.removeWorkspaceMember(nonExistMemberId, requester.getId()))
			.isInstanceOf(WorkspaceMemberNotFoundException.class);
	}

	@Test
	@DisplayName("워크스페이스에 소속되지 않은 멤버를 추방하려는 경우 예외가 발생 한다")
	void kickWorkspaceMember_MemberNotInWorkspaceException() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		WorkspaceMember requester = workspaceRepositoryFixture.addAndSaveMemberToWorkspace(
			requesterMember,
			workspace,
			WorkspaceRole.OWNER
		);

		Long nonExistingWorkspaceMemberId = 999L;

		// when & then
		assertThatThrownBy(
			() -> workspaceMemberCommandService.removeWorkspaceMember(nonExistingWorkspaceMemberId,
				requester.getId()))
			.isInstanceOf(WorkspaceMemberNotFoundException.class);
	}

	@Transactional
	@Test
	@DisplayName("특정 워크스페이스의 멤버의 권한을 변경에 성공하면 변경된 멤버의 정보를 응답으로 반환한다")
	void updateWorkspaceMember_success_returnsUpdateWorkspaceMemberRoleResponse() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"requester",
			"requester123@test.com",
			"password1234!"
		);

		WorkspaceMember requester = WorkspaceMember.addWorkspaceMember(
			requesterMember,
			workspace,
			WorkspaceRole.MANAGER,
			requesterMember.getEmail()
		);

		requester = workspaceMemberRepository.save(requester);

		Member targetMember = memberRepositoryFixture.createAndSaveMember(
			"target",
			"target123@test.com",
			"password1234!"
		);

		WorkspaceMember target = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.VIEWER,
			targetMember.getEmail()
		);

		target = workspaceMemberRepository.save(target);

		UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.MANAGER);

		// when
		UpdateRoleResponse response = workspaceMemberCommandService.updateWorkspaceMemberRole(
			target.getId(),
			requester.getId(),
			request
		);

		// then
		assertThat(response.updatedRole()).isEqualTo(WorkspaceRole.MANAGER);
	}

	@Transactional
	@Test
	@DisplayName("자기 자신의 워크스페이스 멤버 권한을 변경하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateItself() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"requester",
			"requester123@test.com",
			"password1234!"
		);

		WorkspaceMember requester = WorkspaceMember.addWorkspaceMember(
			requesterMember,
			workspace,
			WorkspaceRole.MANAGER,
			requesterMember.getEmail()
		);

		workspaceMemberRepository.save(requester);

		// when & then
		UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.MANAGER);

		assertThatThrownBy(
			() -> workspaceMemberCommandService.updateWorkspaceMemberRole(requester.getId(), requester.getId(),
				request))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 멤버의 권한을 OWNER로 변경하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateToOwner() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"requester",
			"requester123@test.com",
			"password1234!"
		);

		WorkspaceMember requester = WorkspaceMember.addWorkspaceMember(
			requesterMember,
			workspace,
			WorkspaceRole.MANAGER,
			requesterMember.getEmail()
		);

		workspaceMemberRepository.save(requester);

		Member targetMember = memberRepositoryFixture.createAndSaveMember(
			"target",
			"target123@test.com",
			"password1234!"
		);

		WorkspaceMember target = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.VIEWER,
			targetMember.getEmail()
		);

		workspaceMemberRepository.save(target);

		// when & then
		UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.OWNER);

		assertThatThrownBy(
			() -> workspaceMemberCommandService.updateWorkspaceMemberRole(target.getId(), requester.getId(), request))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Transactional
	@Test
	@DisplayName("자기 자신보다 높은 권한을 가진 멤버를 업데이트하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifRequesterTrysToUpdateHigherRoleWorkspaceMember() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"requester",
			"requester123@test.com",
			"password1234!"
		);

		WorkspaceMember requester = WorkspaceMember.addWorkspaceMember(
			requesterMember,
			workspace,
			WorkspaceRole.MANAGER,
			requesterMember.getEmail()
		);

		workspaceMemberRepository.save(requester);

		Member targetMember = memberRepositoryFixture.createAndSaveMember(
			"target",
			"target123@test.com",
			"password1234!"
		);

		WorkspaceMember target = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.OWNER,
			targetMember.getEmail()
		);

		workspaceMemberRepository.save(target);

		// when & then
		UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.MANAGER);

		assertThatThrownBy(
			() -> workspaceMemberCommandService.updateWorkspaceMemberRole(target.getId(), requester.getId(), request))
			.isInstanceOf(ForbiddenOperationException.class);
	}

	@Transactional
	@Test
	@DisplayName("OWNER 권한을 가진 멤버가 다른 멤버를 OWNER 권한으로 업데이트하려고 하면 예외가 발생한다")
	void updateWorkspaceMember_fail_ifOwnerTrysToUpdateWorkspaceMemberToOwner() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"requester",
			"requester123@test.com",
			"password1234!"
		);

		WorkspaceMember requester = WorkspaceMember.addWorkspaceMember(
			requesterMember,
			workspace,
			WorkspaceRole.OWNER,
			requesterMember.getEmail()
		);

		workspaceMemberRepository.save(requester);

		Member targetMember = memberRepositoryFixture.createAndSaveMember(
			"target",
			"target123@test.com",
			"password1234!"
		);

		WorkspaceMember target = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.MANAGER,
			targetMember.getEmail()
		);

		workspaceMemberRepository.save(target);

		// when & then
		UpdateRoleRequest request = new UpdateRoleRequest(WorkspaceRole.OWNER);

		assertThatThrownBy(
			() -> workspaceMemberCommandService.updateWorkspaceMemberRole(target.getId(), requester.getId(), request))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Transactional
	@Test
	@DisplayName("OWNER 권한을 가진 멤버가 다른 멤버로 워크스페이스 소유권 이전을 성공하면 응답이 반환된다")
	void testTransferWorkspaceOwnership_ifSuccess_returnTransferWorkspaceOwnershipResponse() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member requesterMember = memberRepositoryFixture.createAndSaveMember(
			"requester",
			"requester123@test.com",
			"password1234!"
		);

		WorkspaceMember requester = WorkspaceMember.addOwnerWorkspaceMember(
			requesterMember,
			workspace
		);

		workspaceMemberRepository.save(requester);

		Member targetMember = memberRepositoryFixture.createAndSaveMember(
			"target",
			"target123@test.com",
			"password1234!"
		);

		WorkspaceMember target = WorkspaceMember.addWorkspaceMember(
			targetMember,
			workspace,
			WorkspaceRole.MANAGER,
			targetMember.getEmail()
		);

		workspaceMemberRepository.save(target);

		// when
		TransferOwnershipResponse response = workspaceMemberCommandService.transferWorkspaceOwnership(
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
		Member tester = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"password1234!"
		);

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		WorkspaceMember workspaceMember = workspaceMemberRepository.save(
			WorkspaceMember.addCollaboratorWorkspaceMember(tester, workspace)
		);

		String newNickname = "newNickname";
		UpdateNicknameRequest request = new UpdateNicknameRequest(newNickname);

		// when
		UpdateNicknameResponse response = workspaceMemberCommandService.updateNickname(
			workspaceMember.getId(),
			request
		);

		// then
		assertThat(response.updatedNickname()).isEqualTo(newNickname);

		WorkspaceMember updatedMember = workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(tester.getId(), workspace.getCode())
			.orElseThrow();
		assertThat(updatedMember.getNickname()).isEqualTo(newNickname);
	}

	@Test
	@DisplayName("이미 사용 중인 별칭으로 변경 시 예외가 발생한다")
	void updateNickname_Failed_DuplicateNickname() {
		// given
		Member tester = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"password1234!"
		);

		Member existingMember = memberRepositoryFixture.createAndSaveMember(
			"existingMember",
			"existingMember@test.com",
			"password1234!"
		);

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		WorkspaceMember testerWorkspaceMember = workspaceMemberRepository.save(
			WorkspaceMember.addCollaboratorWorkspaceMember(tester, workspace)
		);

		workspaceMemberRepository.save(
			WorkspaceMember.addWorkspaceMember(
				existingMember,
				workspace,
				WorkspaceRole.MEMBER,
				"existingNickname"
			)
		);

		String existingNickname = "existingNickname";
		UpdateNicknameRequest request = new UpdateNicknameRequest(existingNickname);

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.updateNickname(testerWorkspaceMember.getId(), request))
			.isInstanceOfAny(DuplicateResourceException.class);
	}

	@Transactional
	@Test
	@DisplayName("포지션 할당에 성공하면 반환하는 응답에는 할당된 포지션의 이름이 포함된다")
	void assignPosition_Success() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"password1234!"
		);

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Position position = positionRepositoryFixture.createAndSavePosition("BACKEND-DEV", workspace);

		WorkspaceMember workspaceMember = WorkspaceMember.addCollaboratorWorkspaceMember(member, workspace);
		entityManager.flush();

		// When
		AssignPositionResponse response = workspaceMemberCommandService.assignPosition(
			"TESTCODE",
			position.getId(),
			workspaceMember.getId()
		);

		// Then
		assertThat(response.workspaceMemberId()).isEqualTo(workspaceMember.getId());
		assertThat(response.assignedPositions().get(0).name()).isEqualTo(position.getName());

		WorkspaceMember updatedMember = workspaceMemberRepository.findById(workspaceMember.getId()).get();
		assertThat(updatedMember.getWorkspaceMemberPositions().get(0).getPosition()).isEqualTo(position);
	}

	@Transactional
	@Test
	@DisplayName("하나의 워크스페이스 멤버에게 여러가지 포지션의 할당이 가능하다")
	void assignMultiplePositions() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"password1234!"
		);

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Position position1 = positionRepositoryFixture.createAndSavePosition("BACKEND-DEV", workspace);
		Position position2 = positionRepositoryFixture.createAndSavePosition("FRONTEND-DEV", workspace);

		WorkspaceMember workspaceMember = WorkspaceMember.addCollaboratorWorkspaceMember(member, workspace);
		entityManager.flush();

		workspaceMemberCommandService.assignPosition("TESTCODE", position1.getId(), workspaceMember.getId());

		// when
		AssignPositionResponse response = workspaceMemberCommandService.assignPosition("TESTCODE", position2.getId(),
			workspaceMember.getId());

		// then
		assertThat(response.assignedPositions().get(0).name()).isEqualTo("BACKEND-DEV");
		assertThat(response.assignedPositions().get(1).name()).isEqualTo("FRONTEND-DEV");
	}

	@Test
	@DisplayName("존재하지 않는 포지션으로 할당 시도하면 예외 발생")
	void assignPosition_WithNonExistentPosition_ThrowsException() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"password1234!"
		);

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		WorkspaceMember workspaceMember = WorkspaceMember.addCollaboratorWorkspaceMember(member, workspace);

		// When & Then
		assertThatThrownBy(
			() -> workspaceMemberCommandService.assignPosition("TESTCODE", 999L, workspaceMember.getId()))
			.isInstanceOf(ResourceNotFoundException.class);
	}
}
