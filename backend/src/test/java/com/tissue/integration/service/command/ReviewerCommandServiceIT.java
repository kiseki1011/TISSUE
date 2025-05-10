package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import com.tissue.api.review.presentation.dto.request.AddReviewerRequest;
import com.tissue.api.review.presentation.dto.request.RemoveReviewerRequest;
import com.tissue.api.review.presentation.dto.response.ReviewerResponse;
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
	Member ownerMember;
	Member member1;
	Member member2;

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
	@Transactional
	@DisplayName("이슈 참여자(assignees)에 속하면 이슈에 대한 리뷰어(reviewer)를 등록할 수 있다")
	void canAddReviewerIfAssignee() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		issue.addAssignee(workspaceMember1); // add requester as assignee

		AddReviewerRequest request = new AddReviewerRequest(member2.getId());

		// when
		ReviewerResponse response = reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			request.toCommand(),
			member1.getId()
		);

		// then
		assertThat(response.workspaceCode()).isEqualTo(workspace.getCode());
		assertThat(response.issueKey()).isEqualTo(issue.getIssueKey());
		assertThat(response.reviewerMemberId()).isEqualTo(member2.getId());
	}

	@Test
	@DisplayName("워크스페이스에 존재하지 않는 워크스페이스 멤버를 리뷰어로 추가할 수 없다")
	void cannotAddWorkspaceMemberThatDidNotJoinWorkspaceAsReviewer() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long invalidWorkspaceMemberId = 999L; // workspace member that doesn't exist or did not join workspace
		Long invalidMemberId = 999L; // member that doesn't exist or did not join workspace

		AddReviewerRequest request = new AddReviewerRequest(invalidMemberId);

		// when & then
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			request.toCommand(),
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

		AddReviewerRequest request = new AddReviewerRequest(member1.getId());

		reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			request.toCommand(),
			requesterWorkspaceMemberId
		);

		// when & then
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			request.toCommand(),
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

		AddReviewerRequest addReviewerRequest = new AddReviewerRequest(member2.getId());

		reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			addReviewerRequest.toCommand(),
			member1.getId()
		);

		// when
		RemoveReviewerRequest request = new RemoveReviewerRequest(member2.getId());

		ReviewerResponse response = reviewerCommandService.removeReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			request.toCommand(),
			member1.getId()
		);

		// then
		assertThat(response.reviewerMemberId()).isEqualTo(member2.getId());
	}

	@Test
	@Disabled("assignee인지의 여부를 검증하는 로직 주석 처리(Authorization Service로 분리 후에 테스트 활성화)")
	@DisplayName("이슈의 참여자(assignee)가 아니라면 리뷰어를 추가할 수 없다")
	void cannotAddReviewerIfNotAssignee() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId(); // is not assignee of the issue
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		// when & then
		AddReviewerRequest request = new AddReviewerRequest(member2.getId());

		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			request.toCommand(),
			member1.getId()
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

		AddReviewerRequest request = new AddReviewerRequest(member2.getId());

		reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			request.toCommand(),
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
