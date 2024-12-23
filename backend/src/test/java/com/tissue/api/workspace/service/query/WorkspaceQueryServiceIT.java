package com.tissue.api.workspace.service.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.tissue.helper.ServiceIntegrationTestHelper;
import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;

class WorkspaceQueryServiceIT extends ServiceIntegrationTestHelper {

	@BeforeEach
	void setup() {
		// member1 회원가입
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"member1",
			"member1@test.com",
			"member1password!"
		);
		memberCommandService.signup(signupMemberRequest);

		// workspace1, workspace2 생성
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			null
		);
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace2",
			"description2",
			"TEST2222",
			null
		);

		// member1은 workspace1,2에 참여
		workspaceParticipationCommandService.joinWorkspace("TEST1111", new JoinWorkspaceRequest(), 1L);
		workspaceParticipationCommandService.joinWorkspace("TEST2222", new JoinWorkspaceRequest(), 1L);
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
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"member2",
			"member2@test.com",
			"member2password!"
		);
		memberCommandService.signup(signupMemberRequest);

		workspaceParticipationCommandService.joinWorkspace(
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
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"member2",
			"member2@test.com",
			"member2password!"
		);
		memberCommandService.signup(signupMemberRequest);

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail("BADCODE1"))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}

	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스를 조회하면, 워크스페이스를 반환한다")
	void testGetWorkspaceDetail_Success() {
		// given
		String workspaceCode = "TESTCODE";

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member3",
			"member3@test.com",
			"password1234!"
		);

		workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member, workspace, WorkspaceRole.COLLABORATOR);

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
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member3",
			"member3@test.com",
			"password1234!"
		);

		workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member, workspace, WorkspaceRole.COLLABORATOR);

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail(invalidCode))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}
}
