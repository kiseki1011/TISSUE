package com.tissue.api.review.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.review.domain.repository.IssueReviewerRepository;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.exception.NotIssueReviewerException;
import com.tissue.api.review.exception.ReviewNotFoundException;
import com.tissue.api.review.presentation.dto.request.CreateReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewStatusRequest;
import com.tissue.api.review.presentation.dto.response.CreateReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewStatusResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Todo
 *  - 알림 서비스 구현 필요
 *    - 리뷰 상태 변경 -> 모든 리뷰가 작성되었고 전부 APPROVED -> 이슈 assignees에게 알림
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReviewCommandService {

	private final ReviewRepository reviewRepository;
	private final IssueRepository issueRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final IssueReviewerRepository issueReviewerRepository;

	@Transactional
	public CreateReviewResponse createReview(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		CreateReviewRequest request
	) {
		Issue issue = findIssue(workspaceCode, issueKey);

		IssueReviewer issueReviewer = findIssueReviewer(issueKey, reviewerWorkspaceMemberId);

		issue.validateReviewIsCreateable();

		Review review = issueReviewer.addReview(
			request.status(),
			request.title(),
			request.content(),
			issue.getCurrentReviewRound()
		);

		Review savedReview = reviewRepository.save(review);

		updateIssueStatusBasedOnReviewStatus(issue, request.status());

		return CreateReviewResponse.from(savedReview);
	}

	@Transactional
	public UpdateReviewResponse updateReview(
		Long reviewId,
		Long reviewerWorkspaceMemberId,
		UpdateReviewRequest request
	) {
		Review review = findReview(reviewId);
		review.validateIsAuthor(reviewerWorkspaceMemberId);

		review.updateTitle(request.title());
		review.updateContent(request.content());

		return UpdateReviewResponse.from(review);
	}

	@Transactional
	public UpdateReviewStatusResponse updateReviewStatus(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		Long requesterWorkspaceMemberId,
		UpdateReviewStatusRequest request
	) {
		Issue issue = findIssue(workspaceCode, issueKey);
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId, workspaceCode);
		Review review = findReview(reviewId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			review.validateIsAuthor(requesterWorkspaceMemberId);
		}

		review.updateStatus(request.status());
		updateIssueStatusBasedOnReviewStatus(issue, request.status());

		return UpdateReviewStatusResponse.from(review);
	}

	private Issue findIssue(String workspaceCode, String issueKey) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode)
			.orElseThrow(IssueNotFoundException::new);
	}

	// Todo: 굳이 workspaceCode 까지 활용 해야 함? 이미 인터셉터에서 해당 워크스페이스에 속하는지 검사 중.
	private WorkspaceMember findWorkspaceMember(Long id, String workspaceCode) {
		return workspaceMemberRepository.findByIdAndWorkspaceCode(id, workspaceCode)
			.orElseThrow(WorkspaceMemberNotFoundException::new);
	}

	private IssueReviewer findIssueReviewer(String issueKey, Long reviewerWorkspaceMemberId) {
		return issueReviewerRepository.findByIssueKeyAndReviewerId(issueKey, reviewerWorkspaceMemberId)
			.orElseThrow(NotIssueReviewerException::new);
	}

	private Review findReview(Long id) {
		return reviewRepository.findById(id)
			.orElseThrow(ReviewNotFoundException::new);
	}

	private void updateIssueStatusBasedOnReviewStatus(Issue issue, ReviewStatus reviewStatus) {
		// CHANGES_REQUESTED의 경우 자동 상태 변경
		if (reviewStatus == ReviewStatus.CHANGES_REQUESTED) {
			issue.updateStatus(IssueStatus.CHANGES_REQUESTED);
			return;
		}

		/*
		 * Todo
		 *  - 모든 리뷰어가 승인한 경우, 작업자에게 알림
		 *  - 알림 서비스 구현 필요
		 *  - 자동으로 이슈 상태 DONE으로 변경 X
		 *  - 알림은 이벤트 리스너로 구현하는 것이 좋을 듯
		 */
		boolean allApproved = issue.getReviewers().stream()
			.allMatch(reviewer ->
				reviewer.getCurrentReviewStatus(issue.getCurrentReviewRound())
					== ReviewStatus.APPROVED
			);

		if (allApproved) {
			// Todo: 알림 서비스 구현 후 추가
		}
	}
}
