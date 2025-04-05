package com.tissue.api.review.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.event.ReviewSubmittedEvent;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.presentation.dto.request.SubmitReviewRequest;
import com.tissue.api.review.presentation.dto.request.UpdateReviewRequest;
import com.tissue.api.review.presentation.dto.response.SubmitReviewResponse;
import com.tissue.api.review.presentation.dto.response.UpdateReviewResponse;

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
	private final ReviewerReader reviewerReader;
	private final IssueReader issueReader;

	private final ReviewRepository reviewRepository;

	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public SubmitReviewResponse submitReview(
		String workspaceCode,
		String issueKey,
		Long currentWorkspaceMemberId,
		SubmitReviewRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		IssueReviewer issueReviewer = reviewerReader.findByIssueKeyAndWorkspaceMemberId(
			issueKey,
			currentWorkspaceMemberId
		);

		issue.validateCanSubmitReview();

		Review review = issueReviewer.submitReview(
			request.status(),
			request.title(),
			request.content()
		);

		Review savedReview = reviewRepository.save(review);

		// TODO: ReviewSubmittedEvent(이슈의 구독자)
		// TODO: ReviewSubmittedEvent로 Review를 넘기는게 좋을까? 아니면 그냥 id를 넘기는게 좋을까?
		eventPublisher.publishEvent(
			ReviewSubmittedEvent.createEvent(issue, currentWorkspaceMemberId, savedReview.getId())
		);

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

}
