package com.tissue.api.assignee.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.assignee.presentation.dto.response.AddAssigneeResponse;
import com.tissue.api.assignee.presentation.dto.response.RemoveAssigneeResponse;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.helper.ServiceIntegrationTestHelper;

class AssigneeCommandServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;
	WorkspaceMember owner;
	WorkspaceMember workspaceMember1;
	WorkspaceMember workspaceMember2;
	Story issue;

	@BeforeEach
	public void setUp() {
		// create workspace
		workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		// create member
		Member ownerMember = testDataFixture.createMember("owner");
		Member member1 = testDataFixture.createMember("member1");
		Member member2 = testDataFixture.createMember("member2");

		// add workspace members
		owner = testDataFixture.createWorkspaceMember(
			ownerMember,
			workspace,
			WorkspaceRole.OWNER
		);
		workspaceMember1 = testDataFixture.createWorkspaceMember(
			member1,
			workspace,
			WorkspaceRole.MEMBER
		);
		workspaceMember2 = testDataFixture.createWorkspaceMember(
			member2,
			workspace,
			WorkspaceRole.MEMBER
		);

		// create issue
		issue = testDataFixture.createStory(
			workspace,
			"test issue(STORY type)",
			IssuePriority.MEDIUM,
			null
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("이슈에 다수의 이슈 작업자(IssueAssignee)를 등록할 수 있다")
	void canAddMultipleAssigneesToIssue() {
		// given

		// when
		AddAssigneeResponse response1 = assigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			workspaceMember1.getId()
		);

		AddAssigneeResponse response2 = assigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			workspaceMember2.getId()
		);

		// then
		assertThat(response1.workspaceMemberId()).isEqualTo(workspaceMember1.getId());
		assertThat(response2.workspaceMemberId()).isEqualTo(workspaceMember2.getId());
	}

	@Test
	@DisplayName("유효하지 않은 이슈 키에 대해 작업자(IssueAssignee)를 추가할 수 없다")
	void cannotAddAssigneeWithInvalidIssueKey() {
		// given

		// then & when
		assertThatThrownBy(() -> assigneeCommandService.addAssignee(
			workspace.getCode(),
			"INVALIDKEY", // invalid issue key
			workspaceMember1.getId()
		))
			.isInstanceOf(IssueNotFoundException.class);
	}

	@Test
	@DisplayName("이슈에 등록된 작업자(IssueAssignee)를 해제할 수 있다")
	void canRemoveAssigneeFromIssueIfAssignee() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long assigneeWorkspaceMemberId = workspaceMember2.getId();

		// requester of assignee removal must be an assignee
		assigneeCommandService.addAssignee(workspace.getCode(), issue.getIssueKey(), requesterWorkspaceMemberId);
		assigneeCommandService.addAssignee(workspace.getCode(), issue.getIssueKey(), assigneeWorkspaceMemberId);

		// when
		RemoveAssigneeResponse response = assigneeCommandService.removeAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			assigneeWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.workspaceMemberId()).isEqualTo(assigneeWorkspaceMemberId);
	}

	@Test
	@DisplayName("이슈의 작업자(IssueAssignee)에 포함되어 있지 않으면 해당 이슈에 대해 작업자를 해제할 수 없다")
	void cannotRemoveAssigneeFromIssueIfNotAssigneeOfIssue() {
		// given
		Member member3 = testDataFixture.createMember("member3");
		WorkspaceMember workspaceMember3 = testDataFixture.createWorkspaceMember(
			member3,
			workspace,
			WorkspaceRole.MEMBER
		);

		Long assigneeWorkspaceMemberId = workspaceMember2.getId();
		Long requesterWorkspaceMemberId = workspaceMember3.getId();

		assigneeCommandService.addAssignee(workspace.getCode(), issue.getIssueKey(), assigneeWorkspaceMemberId);

		// when & then
		assertThatThrownBy(() -> assigneeCommandService.removeAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			assigneeWorkspaceMemberId,
			requesterWorkspaceMemberId // requester is not assignee
		))
			.isInstanceOf(ForbiddenOperationException.class);
	}
}