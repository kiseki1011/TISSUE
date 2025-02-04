package com.tissue.api.comment.service.command;

import static com.tissue.api.review.domain.enums.ReviewStatus.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.ReviewComment;
import com.tissue.api.comment.presentation.dto.request.CreateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.response.ReviewCommentResponse;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.member.domain.Member;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.Review;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.helper.ServiceIntegrationTestHelper;

class ReviewCommentCommandServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;
	Story issue;
	Review review;
	WorkspaceMember owner;
	WorkspaceMember workspaceMember1;
	WorkspaceMember workspaceMember2;

	@Transactional
	@BeforeEach
	void setUp() {
		workspace = testDataFixture.createWorkspace("test workspace", null, null);

		Member ownerMember = testDataFixture.createMember("owner");
		Member member1 = testDataFixture.createMember("member1");
		Member member2 = testDataFixture.createMember("member2");

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

		issue = testDataFixture.createStory(
			workspace,
			"story issue",
			IssuePriority.MEDIUM,
			null
		);

		// 작업자 등록
		// assigneeCommandService.addAssignee(
		// 	workspaceCode,
		// 	issueKey,
		// 	requesterWorkspaceMemberId
		// );
		// workspaceMember1을 IssueAssignee로 등록
		testDataFixture.addIssueAssignee(issue, workspaceMember1);

		// 리뷰어 등록
		// reviewerCommandService.addReviewer(
		// 	workspaceCode,
		// 	issueKey,
		// 	reviewerWorkspaceMemberId, // testUser2를 리뷰어로 등록
		// 	requesterWorkspaceMemberId // testUser는 요청자
		// );
		// workspaceMember2를 IssueReviewer로 등록
		IssueReviewer reviewer = testDataFixture.addIssueReviewer(issue, workspaceMember2);

		// 이슈 상태를 IN_PROGRESS로 변경
		// issueCommandService.updateIssueStatus(
		// 	workspaceCode,
		// 	issueKey,
		// 	requesterWorkspaceMemberId,
		// 	new UpdateIssueStatusRequest(IssueStatus.IN_PROGRESS)
		// );
		issue.updateStatus(IssueStatus.IN_PROGRESS);

		// 리뷰 요청
		// reviewerCommandService.requestReview(
		// 	workspaceCode,
		// 	issueKey,
		// 	requesterWorkspaceMemberId
		// );
		issue.requestReview();

		// 리뷰 등록
		// CreateReviewRequest createReviewRequest = new CreateReviewRequest(CHANGES_REQUESTED, "Title", "Content");
		//
		// reviewCommandService.createReview(
		// 	workspaceCode,
		// 	issueKey,
		// 	reviewerWorkspaceMemberId,
		// 	createReviewRequest
		// );
		review = testDataFixture.createReview(reviewer, "test review", APPROVED);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@Transactional
	@DisplayName("특정 리뷰에 댓글 작성을 성공한다")
	void createReviewComment_success() {
		// given
		CreateReviewCommentRequest request = new CreateReviewCommentRequest(
			"Test Comment",
			null
		);

		// when
		ReviewCommentResponse response = reviewCommentCommandService.createComment(
			workspace.getCode(),
			issue.getIssueKey(),
			review.getId(),
			request,
			workspaceMember1.getId()
		);

		// then
		assertThat(response.author().workspaceMemberId()).isEqualTo(workspaceMember1.getId());
		assertThat(response.content()).isEqualTo("Test Comment");
	}

	@Test
	@Transactional
	@DisplayName("리뷰 댓글에 대한 대댓글 작성에 성공한다")
	void createReviewReplyComment_success() {
		// given
		// CreateReviewCommentRequest parentCommentRequest = new CreateReviewCommentRequest(
		// 	"Test Comment",
		// 	null
		// );
		//
		// ReviewCommentResponse parentCommentResponse = reviewCommentCommandService.createComment(
		// 	workspaceCode,
		// 	issueKey,
		// 	reviewId,
		// 	parentCommentRequest,
		// 	currentWorkspaceMemberId
		// );

		ReviewComment parentComment = testDataFixture.createReviewComment(
			review,
			"original comment",
			workspaceMember1,
			null
		);

		CreateReviewCommentRequest replyCommentRequest = new CreateReviewCommentRequest(
			"reply comment",
			parentComment.getId()
		);

		// when
		ReviewCommentResponse response = reviewCommentCommandService.createComment(
			workspace.getCode(),
			issue.getIssueKey(),
			review.getId(),
			replyCommentRequest,
			workspaceMember1.getId()
		);

		// then
		assertThat(response.content()).isEqualTo("reply comment");
		assertThat(response.author().workspaceMemberId()).isEqualTo(workspaceMember1.getId());
	}

	@Test
	@Transactional
	@DisplayName("댓글 작성자는 자신의 댓글을 수정할 수 있다")
	void updateReviewComment_byAuthor_success() {
		// given
		// Long currentWorkspaceMemberId = 1L;
		// Long reviewId = 1L;
		//
		// CreateReviewCommentRequest createRequest = new CreateReviewCommentRequest(
		// 	"Test Comment",
		// 	null
		// );
		//
		// ReviewCommentResponse createResponse = reviewCommentCommandService.createComment(
		// 	workspaceCode,
		// 	issueKey,
		// 	reviewId,
		// 	createRequest,
		// 	currentWorkspaceMemberId
		// );
		ReviewComment comment = testDataFixture.createReviewComment(
			review,
			"test comment",
			workspaceMember1,
			null
		);

		UpdateReviewCommentRequest updateRequest = new UpdateReviewCommentRequest("update comment");

		// when
		ReviewCommentResponse updateResponse = reviewCommentCommandService.updateComment(
			issue.getIssueKey(),
			review.getId(),
			comment.getId(),
			updateRequest,
			workspaceMember1.getId()
		);

		// then
		assertThat(updateResponse.content()).isEqualTo("update comment");
	}
}