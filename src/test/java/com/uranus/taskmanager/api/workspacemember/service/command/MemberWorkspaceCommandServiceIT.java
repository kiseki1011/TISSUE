package com.uranus.taskmanager.api.workspacemember.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.exception.AlreadyJoinedWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.JoinWorkspaceResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class MemberWorkspaceCommandServiceIT extends ServiceIntegrationTestHelper {

	private Member member;

	@BeforeEach
	void setUp() {

		Workspace workspace = workspaceRepositoryFixture.createWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		member = memberRepositoryFixture.createMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		workspaceRepositoryFixture.addMemberToWorkspace(
			member,
			workspace,
			WorkspaceRole.COLLABORATOR
		);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스 참여 시 비밀번호가 일치하지 않는 경우 예외가 발생한다")
	void testJoinWorkspace_InvalidPasswordException() {
		// given
		workspaceRepositoryFixture.createWorkspace(
			"Workspace",
			"Workspace with Password",
			"CODE1234",
			"password1234!"
		);

		JoinWorkspaceRequest request = new JoinWorkspaceRequest("WrongPassword1234!");

		// when & then
		assertThatThrownBy(
			() -> memberWorkspaceCommandService.joinWorkspace("CODE1234", request, member.getId()))
			.isInstanceOf(InvalidWorkspacePasswordException.class);
	}

	@Test
	@DisplayName("워크스페이스 참여가 성공하는 경우 워크스페이스 참여 응답을 정상적으로 반환한다")
	void testJoinWorkspace_Success() {
		// given
		JoinWorkspaceRequest request = new JoinWorkspaceRequest(null);

		Member member2 = memberRepositoryFixture.createMember(
			"member2",
			"member2@test.com",
			"password1234!"
		);

		// when
		JoinWorkspaceResponse response = memberWorkspaceCommandService.joinWorkspace(
			"TESTCODE",
			request,
			member2.getId()
		);

		// then
		assertThat(response).isNotNull();
	}

	@Test
	@DisplayName("이미 워크스페이스에 참여하는 멤버가 참여를 시도하는 경우 예외가 발생한다")
	void testJoinWorkspace_isAlreadyMemberTrue() {
		// given
		String workspaceCode = "TESTCODE";
		JoinWorkspaceRequest request = new JoinWorkspaceRequest(null);

		// when & then
		assertThatThrownBy(() -> memberWorkspaceCommandService.joinWorkspace(
				workspaceCode,
				request,
				member.getId()
			)
		).isInstanceOf(AlreadyJoinedWorkspaceException.class);
	}

}