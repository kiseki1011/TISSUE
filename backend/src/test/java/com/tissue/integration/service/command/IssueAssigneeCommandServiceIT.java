package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.issue.presentation.controller.dto.request.AddAssigneeRequest;
import com.tissue.api.issue.presentation.controller.dto.request.RemoveAssigneeRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueAssigneeResponse;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class IssueAssigneeCommandServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;
	WorkspaceMember owner;
	WorkspaceMember workspaceMember1;
	WorkspaceMember workspaceMember2;
	Story issue;

	Member member1;
	Member member2;
	Member ownerMember;

	@BeforeEach
	public void setUp() {
		// create workspace
		workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		// create member
		ownerMember = testDataFixture.createMember("owner");
		member1 = testDataFixture.createMember("member1");
		member2 = testDataFixture.createMember("member2");

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
			LocalDateTime.now().plusDays(7)
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
		Long assigneeWMId1 = workspaceMember1.getId();
		Long assignRequesterWorkspaceMemberId = owner.getId();
		Long assigneeWMId2 = workspaceMember2.getId();

		AddAssigneeRequest addAssigneeRequest1 = new AddAssigneeRequest(member1.getId());
		AddAssigneeRequest addAssigneeRequest2 = new AddAssigneeRequest(member2.getId());

		// when
		IssueAssigneeResponse response1 = issueAssigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			addAssigneeRequest1.toCommand(),
			assignRequesterWorkspaceMemberId
		);

		IssueAssigneeResponse response2 = issueAssigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			addAssigneeRequest2.toCommand(),
			assignRequesterWorkspaceMemberId
		);

		// then
		assertThat(response1.memberId()).isEqualTo(member1.getId());
		assertThat(response2.memberId()).isEqualTo(member2.getId());
	}

	@Test
	@DisplayName("유효하지 않은 이슈 키에 대해 작업자(IssueAssignee)를 추가할 수 없다")
	void cannotAddAssigneeWithInvalidIssueKey() {
		// given
		Long assigneeWMId = workspaceMember1.getId();
		Long assignRequesterWorkspaceMemberId = owner.getId();

		AddAssigneeRequest addAssigneeRequest = new AddAssigneeRequest(member1.getId());

		// then & when
		assertThatThrownBy(() -> issueAssigneeCommandService.addAssignee(
			workspace.getCode(),
			"INVALIDKEY", // invalid issue key
			addAssigneeRequest.toCommand(),
			assignRequesterWorkspaceMemberId
		))
			.isInstanceOf(IssueNotFoundException.class);
	}

	@Test
	@DisplayName("이슈에 등록된 작업자(IssueAssignee)를 해제할 수 있다")
	void canRemoveAssigneeFromIssueIfAssignee() {
		// given
		Long removeRequesterWorkspaceMemberId = workspaceMember1.getId();
		Long assignRequesterWorkspaceMemberId = owner.getId();
		Long assigneeWorkspaceMemberId1 = workspaceMember1.getId();
		Long assigneeWorkspaceMemberId2 = workspaceMember2.getId();

		AddAssigneeRequest addAssigneeRequest1 = new AddAssigneeRequest(member1.getId());
		AddAssigneeRequest addAssigneeRequest2 = new AddAssigneeRequest(member2.getId());

		// requester of assignee removal must be an assignee
		issueAssigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			addAssigneeRequest1.toCommand(),
			assignRequesterWorkspaceMemberId
		);

		issueAssigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			addAssigneeRequest2.toCommand(),
			assigneeWorkspaceMemberId1
		);

		// when
		RemoveAssigneeRequest removeAssigneeRequest = new RemoveAssigneeRequest(member2.getId());

		IssueAssigneeResponse response = issueAssigneeCommandService.removeAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			removeAssigneeRequest.toCommand(),
			removeRequesterWorkspaceMemberId
		);

		// then
		assertThat(response.memberId()).isEqualTo(member2.getId());
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

		AddAssigneeRequest addAssigneeRequest = new AddAssigneeRequest(member1.getId());

		issueAssigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			addAssigneeRequest.toCommand(),
			owner.getId()
		);

		// when & then
		RemoveAssigneeRequest removeAssigneeRequest = new RemoveAssigneeRequest(member1.getId());

		assertThatThrownBy(() -> issueAssigneeCommandService.removeAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			removeAssigneeRequest.toCommand(),
			requesterWorkspaceMemberId // requester is not assignee
		))
			.isInstanceOf(ForbiddenOperationException.class);
	}
}