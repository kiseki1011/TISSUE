package com.uranus.taskmanager.api.workspace.service.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupMemberRequest;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class WorkspaceQueryServiceIT extends ServiceIntegrationTestHelper {

	@BeforeEach
	void setup() {
		// member1 회원가입
		memberCommandService.signup(SignupMemberRequest.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password("member1password!")
			.build());

		// workspace1, workspace2 생성
		workspaceRepositoryFixture.createWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			null
		);
		workspaceRepositoryFixture.createWorkspace(
			"workspace2",
			"description2",
			"TEST2222",
			null
		);

		// member1은 workspace1,2에 참여
		memberWorkspaceCommandService.joinWorkspace("TEST1111", new JoinWorkspaceRequest(), 1L);
		memberWorkspaceCommandService.joinWorkspace("TEST2222", new JoinWorkspaceRequest(), 1L);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Transactional
	@Test
	@DisplayName("해당 워크스페이스에 참여하고 있으면, 워크스페이스의 코드로 상세 정보를 조회할 수 있다")
	void test3() {
		// given
		memberCommandService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		memberWorkspaceCommandService.joinWorkspace(
			"TEST1111",
			new JoinWorkspaceRequest(),
			2L
		);

		// when
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail("TEST1111");

		// then
		assertThat(response.getCode()).isEqualTo("TEST1111");
		assertThat(response.getName()).isEqualTo("workspace1");
	}

	@Transactional
	@Test
	@DisplayName("유효하지 않은 코드로 워크스페이스를 조회하면 예외가 발생한다")
	void test5() {
		// given
		memberCommandService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail("BADCODE1"))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}

	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스를 조회하면, 워크스페이스를 반환한다")
	void testGetWorkspaceDetail_Success() {
		// given
		String workspaceCode = "TESTCODE";

		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);
		Member member = memberRepositoryFixture.createMember(
			"member3",
			"member3@test.com",
			"password1234!"
		);
		workspaceRepositoryFixture.addMemberToWorkspace(member, workspace, WorkspaceRole.COLLABORATOR);

		// when
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(workspaceCode);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getCode()).isEqualTo(workspaceCode);
	}

	@Test
	@DisplayName("유효하지 않은 워크스페이스 코드로 워크스페이스를 조회하면, 예외가 발생한다")
	void testGetWorkspaceDetail_WorkspaceNotFoundException() {
		// given
		String invalidCode = "INVALIDCODE";
		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);
		Member member = memberRepositoryFixture.createMember(
			"member3",
			"member3@test.com",
			"password1234!"
		);
		workspaceRepositoryFixture.addMemberToWorkspace(member, workspace, WorkspaceRole.COLLABORATOR);

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail(invalidCode))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}
}
