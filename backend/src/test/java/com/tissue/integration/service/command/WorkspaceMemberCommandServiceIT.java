package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.position.domain.Position;
import com.tissue.api.team.domain.Team;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateNicknameRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.tissue.api.workspacemember.presentation.dto.response.AssignTeamResponse;
import com.tissue.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateNicknameResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateRoleResponse;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class WorkspaceMemberCommandServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;

	@BeforeEach
	void setUp() {
		// create workspace
		workspace = testDataFixture.createWorkspace("test workspace", null, null);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	/**
	 * 트랜잭션 애노테이션 제거 시 LazyInitializationException 발생
	 * <p>
	 * Hibernate 세션이 닫힌 상태에서 지연 로딩된 속성에 접근하려 할 때 발생
	 * 현재 테스트의 경우, Workspace 엔티티의 workspaceMembers 컬렉션을 액세스하려고 할 때 세션이 종료되어 발생
	 */
	@Test
	@Disabled("아래의 TODO 참고")
	@Transactional
	@DisplayName("멤버를 워크스페이스에서 추방할 수 있다")
	void canRemoveMemberFromWorkspace() {
		// given
		Member requesterMember = testDataFixture.createMember("requester");
		WorkspaceMember requester = WorkspaceMember.addOwnerWorkspaceMember(requesterMember, workspace);

		Member targetMember = testDataFixture.createMember("target");
		WorkspaceMember target = testDataFixture.createWorkspaceMember(targetMember, workspace, WorkspaceRole.MEMBER);

		entityManager.flush();

		// when
		workspaceMemberCommandService.removeWorkspaceMember(workspace.getCode(), targetMember.getId(),
			requesterMember.getId());

		// then
		// TODO: soft delete을 제대로 구현하면 해당 WorkspaceMember를 찾을 수 없어야 함
		// assertThat(workspaceMemberRepository.findById(target.getId()).get().isDeleted()).isTrue();
		assertThat(workspaceMemberRepository.findById(target.getId())).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("유효하지 않은 식별자(id)를 통해 멤버를 워크스페이스에서 추방할 수 없다")
	void cannotKickMemberFromWorkspaceWithInvalidMemberId() {
		// given
		Member requesterMember = testDataFixture.createMember("requester");
		WorkspaceMember requester = WorkspaceMember.addOwnerWorkspaceMember(requesterMember, workspace);

		Long invalidMemberId = 999L;

		entityManager.flush();

		// when & then
		assertThatThrownBy(
			() -> workspaceMemberCommandService.removeWorkspaceMember(workspace.getCode(), invalidMemberId,
				requesterMember.getId()))
			.isInstanceOf(WorkspaceMemberNotFoundException.class);
	}

	@Test
	@Transactional
	@DisplayName("특정 워크스페이스 멤버(WorkspaceMember)의 권한(WorkspaceRole)을 변경할 수 있다")
	void canUpdateWorkspaceRoleOfWorkspaceMember() {
		// given
		Member requesterMember = testDataFixture.createMember("requester");
		WorkspaceMember requester = WorkspaceMember.addOwnerWorkspaceMember(requesterMember, workspace);

		Member targetMember = testDataFixture.createMember("target");
		WorkspaceMember target = testDataFixture.createWorkspaceMember(targetMember, workspace, WorkspaceRole.MEMBER);

		entityManager.flush();

		// when
		UpdateRoleResponse response = workspaceMemberCommandService.updateRole(
			workspace.getCode(),
			targetMember.getId(),
			requesterMember.getId(),
			new UpdateRoleRequest(WorkspaceRole.MANAGER)
		);

		// then
		assertThat(response.updatedRole()).isEqualTo(WorkspaceRole.MANAGER);
	}

	@Test
	@Transactional
	@DisplayName("자기 자신의 워크스페이스 권한(WorkspaceRole)을 변경할 수 없다")
	void cannotUpdateOwnWorkspaceRole() {
		// given
		Member requesterMember = testDataFixture.createMember("requester");

		WorkspaceMember requester = testDataFixture.createWorkspaceMember(requesterMember, workspace,
			WorkspaceRole.OWNER);

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.updateRole(
			workspace.getCode(),
			requesterMember.getId(),
			requesterMember.getId(),
			new UpdateRoleRequest(WorkspaceRole.MANAGER)
		))
			.isInstanceOf(InvalidOperationException.class);
	}

	/**
	 * WorkspaceRole을 OWNER로 변경하기 위해서는 소유권 이전 서비스(transferWorkspaceOwnership)를 호출해야 한다
	 */
	@Test
	@Transactional
	@DisplayName("워크스페이스 멤버의 권한을 OWNER로 변경할 수 없다")
	void cannotUpdateWorkspaceRoleToOwner() {
		// given
		Member requesterMember = testDataFixture.createMember("requester");
		WorkspaceMember requester = WorkspaceMember.addOwnerWorkspaceMember(requesterMember, workspace);

		Member targetMember = testDataFixture.createMember("target");
		WorkspaceMember target = testDataFixture.createWorkspaceMember(targetMember, workspace, WorkspaceRole.MEMBER);

		entityManager.flush();

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.updateRole(
			workspace.getCode(),
			targetMember.getId(),
			requesterMember.getId(),
			new UpdateRoleRequest(WorkspaceRole.OWNER)
		))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Transactional
	@Test
	@DisplayName("자신보다 높은 권한을 가진 워크스페이스 멤버의 권한을 업데이트할 수 없다")
	void cannotUpdateWorkspaceRoleOfWorkspaceMemberThatHasHigherRole() {
		// given
		// requester's role is MANAGER
		Member requesterMember = testDataFixture.createMember("requester");
		WorkspaceMember requester = testDataFixture.createWorkspaceMember(requesterMember, workspace,
			WorkspaceRole.MANAGER);

		// target's role is OWNER (higher than requester)
		Member targetMember = testDataFixture.createMember("target");
		WorkspaceMember target = testDataFixture.createWorkspaceMember(targetMember, workspace, WorkspaceRole.OWNER);

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.updateRole(
			workspace.getCode(),
			targetMember.getId(),
			requesterMember.getId(),
			new UpdateRoleRequest(WorkspaceRole.MANAGER)
		))
			.isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("OWNER 권한을 가져도 다른 워크스페이스 멤버를 OWNER 권한으로 변경할 수 없다")
	void cannotUpdateWorkspaceRoleToOwnerEvenWithOwner() {
		// given
		Member requesterMember = testDataFixture.createMember("requester");
		WorkspaceMember requester = testDataFixture.createWorkspaceMember(requesterMember, workspace,
			WorkspaceRole.OWNER);

		Member targetMember = testDataFixture.createMember("target");
		WorkspaceMember target = testDataFixture.createWorkspaceMember(targetMember, workspace, WorkspaceRole.MANAGER);

		// when & then
		assertThatThrownBy(() -> workspaceMemberCommandService.updateRole(
			workspace.getCode(),
			targetMember.getId(),
			requesterMember.getId(),
			new UpdateRoleRequest(WorkspaceRole.OWNER)
		))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("소유권 이전을 통해 OWNER 권한을 가진 멤버가 다른 멤버에게 OWNER 권한을 넘길 수 있다(기존 OWNER는 MANAGER로 변경)")
	void ownerCanTransferOwnership() {
		// given
		Member requesterMember = testDataFixture.createMember("requester");
		WorkspaceMember requester = testDataFixture.createWorkspaceMember(requesterMember, workspace,
			WorkspaceRole.OWNER);
		requesterMember.increaseMyWorkspaceCount(); // increase workspace count to avoid going below 0

		Member targetMember = testDataFixture.createMember("target");
		WorkspaceMember target = testDataFixture.createWorkspaceMember(targetMember, workspace, WorkspaceRole.MANAGER);

		// when
		TransferOwnershipResponse response = workspaceMemberCommandService.transferWorkspaceOwnership(
			workspace.getCode(),
			targetMember.getId(),
			requesterMember.getId()
		);

		// then
		assertThat(response).isNotNull();
	}

	@Test
	@DisplayName("WorkspaceMember의 표시 이름(displayName)을 업데이트 할 수 있다")
	void canUpdateWorkspaceMemberNickname() {
		// given
		Member member = testDataFixture.createMember("tester");
		WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(member, workspace,
			WorkspaceRole.MEMBER);

		// when
		UpdateNicknameResponse response = workspaceMemberCommandService.updateNickname(
			workspace.getCode(),
			workspaceMember.getId(),
			new UpdateNicknameRequest("newNickname")
		);

		// then
		assertThat(response.updatedNickname()).isEqualTo("newNickname");

		WorkspaceMember updatedMember = workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(member.getId(), workspace.getCode())
			.get();

		assertThat(updatedMember.getDisplayName()).isEqualTo("newNickname");
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스 멤버에게 직책(Position)을 설정할 수 있다")
	void canAssignPositionToWorkspaceMember() {
		// given
		Member member = testDataFixture.createMember("tester");

		Position position = positionRepository.save(Position.builder()
			.workspace(workspace)
			.color(ColorType.BLACK)
			.name("BACKEND")
			.description("backend developer")
			.build());

		WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(
			member,
			workspace,
			WorkspaceRole.MEMBER
		);

		entityManager.flush();

		// When
		AssignPositionResponse response = workspaceMemberCommandService.setPosition(
			workspace.getCode(),
			position.getId(),
			member.getId(),
			member.getId()
		);

		// Then
		assertThat(response.workspaceMemberId()).isEqualTo(workspaceMember.getId());
		// assertThat(respose.positionId()).isEqualTo(position.getId());
	}

	@Test
	@Transactional
	@DisplayName("하나의 워크스페이스 멤버에게 다수의 포지션(Position)을 설정할 수 있다")
	void canAssignMultiplePositions() {
		// given
		Member member = testDataFixture.createMember("tester");

		WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(
			member,
			workspace,
			WorkspaceRole.MEMBER
		);

		Position position1 = positionRepository.save(Position.builder()
			.workspace(workspace)
			.color(ColorType.BLACK)
			.name("BACKEND")
			.description("backend developer")
			.build());

		Position position2 = positionRepository.save(Position.builder()
			.workspace(workspace)
			.color(ColorType.BLACK)
			.name("FRONTEND")
			.description("frontend developer")
			.build());

		entityManager.flush();

		// assign position1 to workspace member
		workspaceMemberCommandService.setPosition(
			workspace.getCode(),
			position1.getId(),
			member.getId(),
			member.getId()
		);

		// when - assign another position(position2) to workspace member
		AssignPositionResponse response = workspaceMemberCommandService.setPosition(
			workspace.getCode(),
			position2.getId(),
			member.getId(),
			member.getId()
		);

		// then
		// TODO: PositionResponse인 response1, response2의 positionId 검증
		// assertThat(response.assignedPositions().get(0).name()).isEqualTo("BACKEND");
		// assertThat(response.assignedPositions().get(1).name()).isEqualTo("FRONTEND");
	}

	@Test
	@DisplayName("존재하지 않는 포지션(Position)을 설정할 수 없다")
	void cannotAssignInvalidPosition() {
		// given
		Member member = testDataFixture.createMember("tester");

		WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(
			member,
			workspace,
			WorkspaceRole.MEMBER
		);

		// When & Then
		assertThatThrownBy(() -> workspaceMemberCommandService.setPosition(
			workspace.getCode(),
			999L, // invalid position id
			member.getId(),
			member.getId()
		)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@Transactional
	@DisplayName("WorkspaceMember에게 소속(Team)을 설정할 수 있다")
	void canAssignTeamToWorkspaceMember() {
		// given
		Member member = testDataFixture.createMember("tester");

		Team team = teamRepository.save(Team.builder()
			.workspace(workspace)
			.color(ColorType.BLACK)
			.name("Payment Team")
			.description("Team for payment")
			.build());

		WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(
			member,
			workspace,
			WorkspaceRole.MEMBER
		);

		entityManager.flush();

		// When
		AssignTeamResponse response = workspaceMemberCommandService.setTeam(
			workspace.getCode(),
			team.getId(),
			member.getId(),
			member.getId()
		);

		// Then
		assertThat(response.workspaceMemberId()).isEqualTo(workspaceMember.getId());
	}
}
