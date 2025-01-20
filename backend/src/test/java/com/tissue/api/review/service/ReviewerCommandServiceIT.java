package com.tissue.api.review.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.helper.ServiceIntegrationTestHelper;

class ReviewerCommandServiceIT extends ServiceIntegrationTestHelper {

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
		AddReviewerResponse response = reviewerCommandService.addReviewer(
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
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
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

		reviewerCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// when & then
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(InvalidOperationException.class);
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

		reviewerCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		// when
		RemoveReviewerResponse response = reviewerCommandService.removeReviewer(
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
		Long requesterWorkspaceMemberId = 3L;

		// when & then
		assertThatThrownBy(() -> reviewerCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		)).isInstanceOf(ForbiddenOperationException.class);
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

		reviewerCommandService.addReviewer(
			workspaceCode,
			issueKey,
			reviewerWorkspaceMemberId,
			requesterWorkspaceMemberId
		);

		issueCommandService.updateIssueStatus(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId,
			new UpdateIssueStatusRequest(IssueStatus.IN_PROGRESS)
		);

		// when
		reviewerCommandService.requestReview(
			workspaceCode,
			issueKey,
			requesterWorkspaceMemberId
		);

		// then
		Issue issue = issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode).orElseThrow();

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.IN_REVIEW);
	}
}
