package com.tissue.api.review.service;

import static com.tissue.api.review.domain.enums.ReviewStatus.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.assignee.exception.UnauthorizedAssigneeModificationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.review.exception.DuplicateReviewInRoundException;
import com.tissue.api.review.exception.DuplicateReviewerException;
import com.tissue.api.review.exception.IssueStatusNotInReviewException;
import com.tissue.api.review.exception.NotIssueReviewerException;
import com.tissue.api.review.presentation.dto.request.CreateReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewStatusRequest;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.CreateReviewResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RequestReviewResponse;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.helper.ServiceIntegrationTestHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ReviewCommandServiceIT extends ServiceIntegrationTestHelper {

	String workspaceCode;
	String issueKey;

	@BeforeEach
	public void setUp() {
		// 테스트 멤버 testuser, testuser2 생성
		SignupMemberResponse testUser = memberFixture.createMember("testuser", "test@test.com");
		SignupMemberResponse testUser2 = memberFixture.createMember("testuser2", "test2@test.com");

		// testuser가 테스트 워크스페이스 생성
		CreateWorkspaceResponse createWorkspace = workspaceFixture.createWorkspace(testUser.memberId());

		workspaceCode = createWorkspace.code();

		// testUser2를 테스트 워크스페이스에 참가
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser2.memberId());

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
	@DisplayName("리뷰어 추가에 성공하면 AddReviewerResponse를 반환한다")
	void addReviewer_success_returnAddReviewerResponse() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		WorkspaceMember reviewer = workspaceMemberRepository.findByMemberIdAndWorkspaceCode(reviewerWorkspaceMemberId,
				workspaceCode)
			.orElseThrow();

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		// when
		AddReviewerResponse response = reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.reviewerId()).isEqualTo(reviewer.getId());
		assertThat(response.reviewerNickname()).isEqualTo(reviewer.getNickname());
	}

	@Test
	@DisplayName("워크스페이스에 존재하지 않는 멤버를 리뷰어로 추가하려고 시도하면 예외가 발생한다")
	void addReviewer_thatIsNotInWorkspace_throwsException() {
		// given
		Long invalidWorkspaceMemberId = 999L;
		Long requesterWorkspaceMemberId = 1L;

		// when & then
		assertThatThrownBy(() -> reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			invalidWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(WorkspaceMemberNotFoundException.class);
	}

	@Test
	@DisplayName("이미 리뷰어로 등록된 멤버를 리뷰어로 추가하려고 시도하면 예외가 발생한다")
	void addReviewer_thatIsAlreadyReviewer_throwsException() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// when & then
		assertThatThrownBy(() -> reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(DuplicateReviewerException.class);
	}

	@Test
	@DisplayName("리뷰어 해제에 성공하면 RemoveReviewerResponse를 반환한다")
	void removeReviewer_success_returnsRemoveReviewerResponse() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// when
		RemoveReviewerResponse response = reviewCommandService.removeReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.workspaceMemberId()).isEqualTo(reviewerWorkspaceMemberId);
	}

	@Test
	@DisplayName("이슈의 assignee에 포함되지 않았지만 리뷰어 추가를 시도하는 경우 예외가 발생한다")
	void addReviewer_whenNotAssignee_throwsException() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		// when & then
		assertThatThrownBy(() -> reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(UnauthorizedAssigneeModificationException.class);
	}

	@Test
	@DisplayName("리뷰 요청이 성공하면 이슈의 상태는 IN_REVIEW로 변한다")
	void fistReviewRequest_success_currentReviewRound_isOne() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// when
		reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		// then
		Issue issue = issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode).orElseThrow();

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.IN_REVIEW);
	}

	@Test
	@DisplayName("이슈가 IN_REVIEW 상태가 아닐때 리뷰 작성을 시도하면 예외가 발생한다")
	void createReview_whenIssueIsNotInReview_throwsException() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		CreateReviewRequest request = new CreateReviewRequest(APPROVED, "Title", "Content");

		// when & then
		assertThatThrownBy(
			() -> reviewCommandService.createReview(workspaceCode, issueKey, reviewerWorkspaceMemberId, request))
			.isInstanceOf(IssueStatusNotInReviewException.class);

	}

	@Test
	@DisplayName("이슈에 대해 리뷰 신청을 통해 IN_REVIEW 상태로 전환되어야 리뷰 작성을 할 수 있다")
	void test() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode, issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		CreateReviewRequest request = new CreateReviewRequest(APPROVED, "Title", "Content");

		// when
		CreateReviewResponse response = reviewCommandService.createReview(workspaceCode, issueKey,
			reviewerWorkspaceMemberId, request);

		// then
		assertThat(response.reviewerId()).isEqualTo(reviewerWorkspaceMemberId);
		assertThat(response.status()).isEqualTo(APPROVED);
	}

	@Test
	@DisplayName("리뷰 상태를 CHANGES_REQUESTED로 설정해서 리뷰 작성에 성공하면, 이슈의 상태도 CHANGES_REQUESTED로 변한다")
	void createReview_success_ifReviewStatusIsChangesRequested_issueStatusUpdatedToChangesRequested() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		CreateReviewRequest request = new CreateReviewRequest(CHANGES_REQUESTED, "Title", "Content");

		// when
		reviewCommandService.createReview(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			request
		);

		// then
		Issue issue = issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode)
			.orElseThrow();

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.CHANGES_REQUESTED);
	}

	@Test
	@DisplayName("이슈의 최초 리뷰 요청이 성공하면 이슈의 currentReviewRound는 1이어야 한다")
	void firstReviewRequest_currentReviewRound_shouldBeOne() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		CreateReviewRequest request = new CreateReviewRequest(CHANGES_REQUESTED, "Title", "Content");

		// when
		reviewCommandService.createReview(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			request
		);

		// then
		Issue issue = issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode)
			.orElseThrow();

		assertThat(issue.getCurrentReviewRound()).isEqualTo(1);
	}

	@Transactional
	@Test
	@DisplayName("특정 이슈의 리뷰 재요청이 성공하면 이슈의 currentReviewRound가 1 증가해야 한다")
	void secondReviewRequest_currentReviewRound_increaseByOne() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		CreateReviewRequest createReviewRequest = new CreateReviewRequest(PENDING, "Title", "Content");

		CreateReviewResponse createReview = reviewCommandService.createReview(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			createReviewRequest
		);

		UpdateReviewStatusRequest updateReviewStatusRequest = new UpdateReviewStatusRequest(CHANGES_REQUESTED);

		reviewCommandService.updateReviewStatus(
			workspaceCode,
			issueKey,
			createReview.reviewId(),
			reviewerWorkspaceMemberId,
			updateReviewStatusRequest
		);

		// when
		RequestReviewResponse response = reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.currentReviewRound()).isEqualTo(2);
	}

	@Test
	@DisplayName("리뷰어가 아닌데 해당 이슈의 리뷰 작성을 시도하면 예외가 발생한다")
	void createReview_whenNotReviewer_throwsException() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		CreateReviewRequest createReviewRequest = new CreateReviewRequest(PENDING, "Title", "Content");

		// when & then
		assertThatThrownBy(() -> reviewCommandService.createReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId, // 리뷰어가 아닌 자가 리뷰 생성 시도
			createReviewRequest
		)).isInstanceOf(NotIssueReviewerException.class);
	}

	@Test
	@DisplayName("리뷰 작성을 시도할 때, 해당 리뷰 라운드에 이미 작성한 리뷰가 존재하면 예외가 발생한다")
	void createReview_ifReviewExistsForCurrentRound_throwsException() {
		// given
		Long reviewerWorkspaceMemberId = 2L;
		Long requesterWorkspaceMemberId = 1L;

		assigneeCommandService.addAssignee(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		reviewCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		CreateReviewRequest createReviewRequest = new CreateReviewRequest(PENDING, "Title", "Content");

		reviewCommandService.createReview(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			createReviewRequest
		);

		// when & then
		assertThatThrownBy(() -> reviewCommandService.createReview(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			createReviewRequest
		)).isInstanceOf(DuplicateReviewInRoundException.class);
	}
}