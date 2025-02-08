package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.response.JoinWorkspaceResponse;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class WorkspaceParticipationCommandServiceIT extends ServiceIntegrationTestHelper {

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

	@Test
	@DisplayName("유효한 워크스페이스 코드를 통해 워크스페이스에 참여할 수 있다")
	void canJoinWorkspaceWithValidWorkspaceCode() {
		// given
		Member member = testDataFixture.createMember("tester");

		// when
		JoinWorkspaceResponse response = workspaceParticipationCommandService.joinWorkspace(
			workspace.getCode(),
			member.getId()
		);

		// then
		assertThat(response).isNotNull();
	}

	@Test
	@DisplayName("이미 워크스페이스에 참여한 멤버는 다시 참여하는 것이 불가능")
	void testJoinWorkspace_isAlreadyMemberTrue() {
		// given
		Member member = testDataFixture.createMember("tester");

		// assume member already joined the workspace
		testDataFixture.createWorkspaceMember(member, workspace, WorkspaceRole.MEMBER);

		// when & then
		assertThatThrownBy(
			() -> workspaceParticipationCommandService.joinWorkspace(workspace.getCode(), member.getId()))
			.isInstanceOf(InvalidOperationException.class);
	}

}