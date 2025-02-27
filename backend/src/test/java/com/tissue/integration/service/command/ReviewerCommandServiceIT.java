package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.member.domain.Member;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class ReviewerCommandServiceIT extends ServiceIntegrationTestHelper {

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
	@Transactional
	@DisplayName("이슈 참여자(assignees)에 속하면 이슈에 대한 리뷰어(reviewer)를 등록할 수 있다")
	void canAddReviewerIfAssignee() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		issue.addAssignee(workspaceMember1); // add requester as assignee

		// when
		AddReviewerResponse response = reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.reviewerId()).isEqualTo(reviewerWorkspaceMemberId);
		assertThat(response.reviewerNickname()).isEqualTo(workspaceMember2.getNickname());
	}

	@Test
	@DisplayName("워크스페이스에 존재하지 않는 워크스페이스 멤버를 리뷰어로 추가할 수 없다")
	void cannotAddWorkspaceMemberThatDidNotJoinWorkspaceAsReviewer() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long invalidWorkspaceMemberId = 999L; // workspace member that doesn't exist or did not join workspace

		// when & then
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			invalidWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(WorkspaceMemberNotFoundException.class);
	}

	@Test
	@Transactional
	@DisplayName("이미 리뷰어로 등록된 멤버를 다시 리뷰어로 등록할 수 없다")
	void cannotAddReviewerThatIsAlreadyReviewer() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		issue.addAssignee(workspaceMember1); // add requester as assignee

		reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// when & then
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("이슈 참여자(assignee)에 속하면 리뷰어(reviewer)를 해제할 수 있다")
	void canRemoveReviewerIfAssignee() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		issue.addAssignee(workspaceMember1); // add requester as assignee

		reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// when
		RemoveReviewerResponse response = reviewerCommandService.removeReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.workspaceMemberId()).isEqualTo(reviewerWorkspaceMemberId);
	}

	@Test
	@DisplayName("이슈의 참여자(assignee)가 아니라면 리뷰어를 추가할 수 없다")
	void cannotAddReviewerIfNotAssignee() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId(); // is not assignee of the issue
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		// when & then
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("리뷰 요청이 성공하면 이슈의 상태는 IN_REVIEW로 변한다")
	void ifReviewRequestSuccess_IssueStatusChangesTo_InReview() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		issue.addAssignee(workspaceMember1); // add requester as assignee

		reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		issue.updateStatus(IssueStatus.IN_PROGRESS);

		// when
		reviewerCommandService.requestReview(
			workspace.getCode(),
			issue.getIssueKey(),
			requesterWorkspaceMemberId
		);

		// then
		Issue foundIssue = issueRepository.findByIssueKeyAndWorkspaceCode(issue.getIssueKey(), workspace.getCode())
			.get();

		assertThat(foundIssue.getStatus()).isEqualTo(IssueStatus.IN_REVIEW);
	}
}
