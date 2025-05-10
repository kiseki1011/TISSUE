package com.tissue.integration.service.command;

import static com.tissue.api.review.domain.enums.ReviewStatus.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.assignee.presentation.dto.request.AddAssigneeRequest;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.member.domain.Member;
import com.tissue.api.review.presentation.dto.request.AddReviewerRequest;
import com.tissue.api.review.presentation.dto.request.SubmitReviewRequest;
import com.tissue.api.review.presentation.dto.response.ReviewResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ReviewCommandServiceIT extends ServiceIntegrationTestHelper {

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
		// Member ownerMember = testDataFixture.createMember("owner");
		// Member member1 = testDataFixture.createMember("member1");
		// Member member2 = testDataFixture.createMember("member2");
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
	@DisplayName("리뷰를 작성하기 위해서는 해당 이슈가 IN_REVIEW 상태이어야 한다")
	void toCreateReviewIssueStatusMustBe_InReview() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long assigneeWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		AddAssigneeRequest addAssigneeRequest = new AddAssigneeRequest(member1.getId());

		assigneeCommandService.addAssignee(
			workspace.getCode(),
			issue.getIssueKey(),
			addAssigneeRequest.toCommand(),
			owner.getId()
		);

		AddReviewerRequest addReviewerRequest = new AddReviewerRequest(member2.getId());

		reviewerCommandService.addReviewer(
			workspace.getCode(),
			issue.getIssueKey(),
			addReviewerRequest.toCommand(),
			member1.getId()
		);

		SubmitReviewRequest request = new SubmitReviewRequest(APPROVED, "test review", "test review");

		// when & then
		assertThatThrownBy(
			() -> reviewCommandService.submitReview(
				workspace.getCode(),
				issue.getIssueKey(),
				reviewerWorkspaceMemberId,
				new SubmitReviewRequest(APPROVED, "test review", "test review")
			)
		).isInstanceOf(InvalidOperationException.class);

	}

	@Test
	@Transactional
	@DisplayName("이슈가 IN_REVIEW 상태이면, 리뷰어는 리뷰 작성을 시작할 수 있다")
	void reviewerCanCreateReviewIfIssueStatusIs_InReview() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		testDataFixture.addIssueAssignee(issue, workspaceMember1);
		testDataFixture.addIssueReviewer(issue, workspaceMember2);

		issue.updateStatus(IssueStatus.IN_PROGRESS);
		issue.requestReview();

		// when
		ReviewResponse response = reviewCommandService.submitReview(
			workspace.getCode(),
			issue.getIssueKey(),
			member2.getId(),
			new SubmitReviewRequest(APPROVED, "test review", "test review")
		);

		// then
		assertThat(response.workspaceCode()).isEqualTo(workspace.getCode());
		assertThat(response.issueKey()).isEqualTo(issue.getIssueKey());
	}

	@Test
	@Transactional
	@DisplayName("이슈의 최초 리뷰 요청이 성공하면 이슈의 현재 리뷰 라운드(currentReviewRound)는 1이어야 한다")
	void forFirstReviewRequest_CurrentReviewRoundShouldBeOne() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		testDataFixture.addIssueAssignee(issue, workspaceMember1);
		testDataFixture.addIssueReviewer(issue, workspaceMember2);

		issue.updateStatus(IssueStatus.IN_PROGRESS);
		issue.requestReview();

		// when
		reviewCommandService.submitReview(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			new SubmitReviewRequest(CHANGES_REQUESTED, "test review", "test review")
		);

		// then
		Issue foundIssue = issueRepository.findByIssueKeyAndWorkspaceCode(issue.getIssueKey(), workspace.getCode())
			.get();

		assertThat(foundIssue.getCurrentReviewRound()).isEqualTo(1);
	}

	// @Test
	// @Transactional
	// @DisplayName("리뷰 재요청이 성공하면 해당 이슈의 현재 리뷰 라운드(currentReviewRound)는 1 증가해야 한다")
	// void forSecondReviewRequest_CurrentReviewRound_ShouldBeTwo() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	Long reviewerWorkspaceMemberId = workspaceMember2.getId();
	//
	// 	testDataFixture.addIssueAssignee(issue, workspaceMember1);
	// 	testDataFixture.addIssueReviewer(issue, workspaceMember2);
	//
	// 	issue.updateStatus(IssueStatus.IN_PROGRESS);
	// 	issue.requestReview(); // first review request
	//
	// 	SubmitReviewResponse submitReviewResponse = reviewCommandService.submitReview(
	// 		workspace.getCode(),
	// 		issue.getIssueKey(),
	// 		reviewerWorkspaceMemberId,
	// 		new SubmitReviewRequest(COMMENT, "test review", "test review")
	// 	);
	//
	// 	// change review status to CHANGES_REQUESTED
	// 	reviewCommandService.updateReviewStatus(
	// 		workspace.getCode(),
	// 		issue.getIssueKey(),
	// 		submitReviewResponse.reviewId(),
	// 		reviewerWorkspaceMemberId,
	// 		new UpdateReviewStatusRequest(CHANGES_REQUESTED)
	// 	);
	//
	// 	// when
	// 	RequestReviewResponse response = reviewerCommandService.requestReview(
	// 		workspace.getCode(),
	// 		issue.getIssueKey(),
	// 		requesterWorkspaceMemberId
	// 	);
	//
	// 	// then - issue's current review round should increase by 1
	// 	assertThat(response.currentReviewRound()).isEqualTo(2);
	// }

	@Test
	@Transactional
	@DisplayName("리뷰어가 아닌자는 해당 이슈에 대해 리뷰 작성을 하지 못한다")
	void cannotCreateReviewIfNotReviewer() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long notReviewerWorkspaceMemberId = 999L;

		testDataFixture.addIssueAssignee(issue, workspaceMember1);
		testDataFixture.addIssueReviewer(issue, workspaceMember2);

		issue.updateStatus(IssueStatus.IN_PROGRESS);
		issue.requestReview();

		// when & then
		assertThatThrownBy(() -> reviewCommandService.submitReview(
			workspace.getCode(),
			issue.getIssueKey(),
			member1.getId(), // workspace member that is not a reviewer
			new SubmitReviewRequest(COMMENT, "test review", "test review")
		)).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("해당 리뷰 라운드에 이미 작성한 리뷰가 존재하면 리뷰를 작성할 수 없다")
	void cannotCreateReviewIfReviewExistsForCurrentReviewRound() {
		// given
		Long requesterWorkspaceMemberId = workspaceMember1.getId();
		Long reviewerWorkspaceMemberId = workspaceMember2.getId();

		testDataFixture.addIssueAssignee(issue, workspaceMember1);
		testDataFixture.addIssueReviewer(issue, workspaceMember2);

		issue.updateStatus(IssueStatus.IN_PROGRESS);
		issue.requestReview();

		// create review for current round
		reviewCommandService.submitReview(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			new SubmitReviewRequest(COMMENT, "test review", "test review")
		);

		// when & then
		assertThatThrownBy(() -> reviewCommandService.submitReview(
			workspace.getCode(),
			issue.getIssueKey(),
			reviewerWorkspaceMemberId,
			new SubmitReviewRequest(COMMENT, "test review", "test review")
		)).isInstanceOf(InvalidOperationException.class);
	}
}