package com.tissue.api.workspacemember.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.exception.AlreadyJoinedWorkspaceException;
import com.tissue.api.workspacemember.presentation.dto.response.JoinWorkspaceResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class WorkspaceParticipationCommandServiceIT extends ServiceIntegrationTestHelper {

	private Member member;

	@BeforeEach
	void setUp() {

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		workspaceRepositoryFixture.addAndSaveMemberToWorkspace(
			member,
			workspace,
			WorkspaceRole.MEMBER
		);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스 참여가 성공하는 경우 워크스페이스 참여 응답을 정상적으로 반환한다")
	void testJoinWorkspace_Success() {
		// given
		Member member2 = memberRepositoryFixture.createAndSaveMember(
			"member2",
			"member2@test.com",
			"password1234!"
		);

		// when
		JoinWorkspaceResponse response = workspaceParticipationCommandService.joinWorkspace(
			"TESTCODE",
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

		// when & then
		assertThatThrownBy(() -> workspaceParticipationCommandService.joinWorkspace(
				workspaceCode,
				member.getId()
			)
		).isInstanceOf(AlreadyJoinedWorkspaceException.class);
	}

}