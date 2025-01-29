package com.tissue.api.comment.service.command;

import static com.tissue.api.review.domain.enums.ReviewStatus.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.comment.presentation.dto.request.CreateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.response.ReviewCommentResponse;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateStoryRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.review.presentation.dto.request.CreateReviewRequest;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class ReviewCommentCommandServiceIT extends ServiceIntegrationTestHelper {

	String workspaceCode;
	String issueKey;

	@BeforeEach
	void setUp() {
		// 멤버 생성
		SignupMemberResponse testUser = memberFixture.createMember("testuser", "test@test.com");
		SignupMemberResponse testUser2 = memberFixture.createMember("testuser2", "test2@test.com");
		SignupMemberResponse testUser3 = memberFixture.createMember("testuser3", "test3@test.com");

		Long requesterWorkspaceMemberId = 1L; // testUser
		Long reviewerWorkspaceMemberId = 2L; // testUser2

		// 워크스페이스 생성
		CreateWorkspaceResponse createWorkspace = workspaceFixture.createWorkspace(testUser.memberId());

		workspaceCode = createWorkspace.code();

		// 멤버가 워크스페이스에 참가
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser2.memberId());
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser3.memberId());

		// Story 타입 이슈 생성
		CreateStoryRequest createStoryRequest = CreateStoryRequest.builder()
			.title("Test Story")
			.content("Test Story")
			.userStory("Test Story User Story")
			.build();

		CreateStoryResponse response = (CreateStoryResponse)issueCommandService.createIssue(
			workspaceCode,
			createStoryRequest
		);

		issueKey = response.issueKey();

		// 작업자 등록
		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		// 리뷰어 등록
		reviewerCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId, // testUser2를 리뷰어로 등록
			requesterWorkspaceMemberId // testUser는 요청자
		);

		// 이슈 상태를 IN_PROGRESS로 변경
		issueCommandService.updateIssueStatus(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId,
			new UpdateIssueStatusRequest(IssueStatus.IN_PROGRESS)
		);

		// 리뷰 요청
		reviewerCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		// 리뷰 등록
		CreateReviewRequest createReviewRequest = new CreateReviewRequest(CHANGES_REQUESTED, "Title", "Content");

		reviewCommandService.createReview(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			createReviewRequest
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("특정 리뷰에 댓글 작성을 성공한다")
	void createReviewComment_success() {
		// given
		Long currentWorkspaceMemberId = 1L;
		Long reviewId = 1L;

		CreateReviewCommentRequest request = new CreateReviewCommentRequest(
			"Test Comment",
			null
		);

		// when
		ReviewCommentResponse response = reviewCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			reviewId,
			request,
			currentWorkspaceMemberId
		);

		// then
		assertThat(response.author().workspaceMemberId()).isEqualTo(currentWorkspaceMemberId);
		assertThat(response.content()).isEqualTo("Test Comment");
	}

	@Test
	@DisplayName("리뷰 댓글에 대한 대댓글 작성에 성공한다")
	void createReviewReplyComment_success() {
		// given
		Long currentWorkspaceMemberId = 1L;
		Long reviewId = 1L;

		CreateReviewCommentRequest parentCommentRequest = new CreateReviewCommentRequest(
			"Test Comment",
			null
		);

		ReviewCommentResponse parentCommentResponse = reviewCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			reviewId,
			parentCommentRequest,
			currentWorkspaceMemberId
		);

		CreateReviewCommentRequest replyCommentRequest = new CreateReviewCommentRequest(
			"Reply Comment",
			parentCommentResponse.id()
		);

		// when
		ReviewCommentResponse response = reviewCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			reviewId,
			replyCommentRequest,
			currentWorkspaceMemberId
		);

		// then
		assertThat(response.content()).isEqualTo("Reply Comment");
		assertThat(response.author().workspaceMemberId()).isEqualTo(currentWorkspaceMemberId);
	}

	@Test
	@DisplayName("댓글 작성자는 자신의 댓글을 수정할 수 있다")
	void updateReviewComment_byAuthor_success() {
		// given
		Long currentWorkspaceMemberId = 1L;
		Long reviewId = 1L;

		CreateReviewCommentRequest createRequest = new CreateReviewCommentRequest(
			"Test Comment",
			null
		);

		ReviewCommentResponse createResponse = reviewCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			reviewId,
			createRequest,
			currentWorkspaceMemberId
		);

		UpdateReviewCommentRequest updateRequest = new UpdateReviewCommentRequest("Update Comment");

		ReviewCommentResponse updateResponse = reviewCommentCommandService.updateComment(
			workspaceCode,
			issueKey,
			reviewId,
			createResponse.id(),
			updateRequest,
			currentWorkspaceMemberId
		);

		// then
		assertThat(updateResponse.content()).isEqualTo("Update Comment");
	}
}