package com.tissue.api.review.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.repository.IssueReviewerRepository;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.presentation.dto.request.SubmitReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewRequest;
import com.tissue.api.review.presentation.dto.response.SubmitReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewResponse;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

/**
 * Todo
 *  - 알림 서비스 구현 필요
 *    - 리뷰 상태 변경 -> 모든 리뷰가 작성되었고 전부 APPROVED -> 이슈 assignees에게 알림
 */
@Service
@RequiredArgsConstructor
public class ReviewCommandService {

	private final ReviewReader reviewReader;
	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;

	private final ReviewRepository reviewRepository;
	private final IssueReviewerRepository issueReviewerRepository;

	@Transactional
	public SubmitReviewResponse submitReview(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		SubmitReviewRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		IssueReviewer issueReviewer = findIssueReviewer(issueKey, reviewerWorkspaceMemberId);

		issue.validateCanSubmitReview();

		Review review = issueReviewer.submitReview(
			request.status(),
			request.title(),
			request.content()
		);

		Review savedReview = reviewRepository.save(review);

		// Todo: 이슈 상태 변경을 리뷰 제출(생성)안에 캡슐화하는 것이 좋을까?
		// updateIssueStatusBasedOnReviewStatus(issue, request.status());

		return SubmitReviewResponse.from(savedReview);
	}

	@Transactional
	public UpdateReviewResponse updateReview(
		Long reviewId,
		Long reviewerWorkspaceMemberId,
		UpdateReviewRequest request
	) {
		Review review = reviewReader.findReview(reviewId);
		review.validateIsAuthor(reviewerWorkspaceMemberId);

		review.updateTitle(request.title());
		review.updateContent(request.content());

		return UpdateReviewResponse.from(review);
	}

	private IssueReviewer findIssueReviewer(String issueKey, Long reviewerWorkspaceMemberId) {
		return issueReviewerRepository.findByIssueKeyAndReviewerId(issueKey, reviewerWorkspaceMemberId)
			.orElseThrow(() -> new ForbiddenOperationException(String.format(
				"Must be a reviewer to create a review. issueKey: %s, workspaceMemberId: %d",
				issueKey, reviewerWorkspaceMemberId)));
	}

	// private void updateIssueStatusBasedOnReviewStatus(Issue issue, ReviewStatus reviewStatus) {
	// 	// CHANGES_REQUESTED의 경우 자동 상태 변경
	// 	if (reviewStatus == ReviewStatus.CHANGES_REQUESTED) {
	// 		issue.updateStatus(IssueStatus.CHANGES_REQUESTED);
	// 		return;
	// 	}
	//
	// 	/*
	// 	 * Todo
	// 	 *  - 모든 리뷰어가 승인한 경우, 작업자에게 알림
	// 	 *  - 알림 서비스 구현 필요
	// 	 *  - 자동으로 이슈 상태 DONE으로 변경 X
	// 	 *  - 알림은 이벤트 리스너로 구현하는 것이 좋을 듯
	// 	 */
	// 	boolean isApproved = issue.getReviewers().stream()
	// 		.allMatch(reviewer ->
	// 			reviewer.getCurrentReviewStatus(issue.getCurrentReviewRound()) != ReviewStatus.CHANGES_REQUESTED
	// 		);
	//
	// 	if (isApproved) {
	// 		// Todo: 알림 서비스 구현 후 추가
	// 	}
	// }
}
