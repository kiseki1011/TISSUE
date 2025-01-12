package com.tissue.api.assignee.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.assignee.exception.UnauthorizedAssigneeModificationException;
import com.tissue.api.assignee.presentation.dto.response.AddAssigneeResponse;
import com.tissue.api.assignee.presentation.dto.response.RemoveAssigneeResponse;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class AssigneeCommandServiceIT extends ServiceIntegrationTestHelper {

	String workspaceCode;
	String issueKey;

	@BeforeEach
	public void setUp() {
		// 테스트 멤버 testUser, testUser2, testUser3 생성
		SignupMemberResponse testUser = memberFixture.createMember("testuser", "test@test.com");
		SignupMemberResponse testUser2 = memberFixture.createMember("testuser2", "test2@test.com");
		SignupMemberResponse testUser3 = memberFixture.createMember("testuser3", "test3@test.com");

		// testuser가 테스트 워크스페이스 생성
		CreateWorkspaceResponse createWorkspace = workspaceFixture.createWorkspace(testUser.memberId());

		workspaceCode = createWorkspace.code();

		// testUser2, testUser3 테스트 워크스페이스에 참가
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser2.memberId());
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser3.memberId());

		// 테스트 워크스페이스에 Story 추가
		CreateStoryResponse createdStory = (CreateStoryResponse)issueFixture.createStory(
			workspaceCode,
			"Test Story",
			null
		);

		issueKey = createdStory.issueKey();
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("이슈의 작업자 추가에 성공하면 추가된 작업자의 WORKSPACE_MEMBER_ID가 반환 응답에 포함된다")
	void addAssignee_success_returnsAddAssigneeResponse() {
		// given
		Long assigneeWorkspaceMemberId = 2L;

		// when
		AddAssigneeResponse response = assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			assigneeWorkspaceMemberId
		);

		// then
		assertThat(response.workspaceMemberId()).isEqualTo(2L);
	}

	@Test
	@DisplayName("존재하지 않는 이슈에 작업자 추가를 시도하면 예외가 발생한다")
	void addAssignee_toNotExistingIssue_throwsException() {
		// given
		Long assigneeWorkspaceMemberId = 2L;

		// then & when
		assertThatThrownBy(() -> assigneeCommandService.addAssignee(
			workspaceCode,
			"INVALID-ISSUE",
			assigneeWorkspaceMemberId
		)).isInstanceOf(IssueNotFoundException.class);
	}

	@Test
	@DisplayName("등록된 작업자 해제에 성공하면 해제된 작업자의 WORKSPACE_MEMBER_ID가 반환 응답에 포함된다")
	void removeAssignee_success_returnsRemoveAssigneeResponse() {
		// given
		Long assigneeWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		// 작업자(assignee) 해제의 요청자도 assignees에 포함되어 있어야 성공한다
		assigneeCommandService.addAssignee(workspaceCode, issueKey, requesterWorkspaceMemberId);
		assigneeCommandService.addAssignee(workspaceCode, issueKey, assigneeWorkspaceMemberId);

		// when
		RemoveAssigneeResponse response = assigneeCommandService.removeAssignee(
			workspaceCode,
			issueKey,
			2L,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.workspaceMemberId()).isEqualTo(2L);
	}

	@Test
	@DisplayName("이슈의 작업자에 포함되어 있지 않으면서 해당 이슈의 작업자 해제를 시도하면 예외가 발생한다")
	void test() {
		// given
		Long assigneeWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 3L;

		assigneeCommandService.addAssignee(workspaceCode, issueKey, assigneeWorkspaceMemberId);

		// when & then
		assertThatThrownBy(() -> assigneeCommandService.removeAssignee(
			workspaceCode,
			issueKey,
			2L,
			requesterWorkspaceMemberId
		)).isInstanceOf(UnauthorizedAssigneeModificationException.class);
	}
}