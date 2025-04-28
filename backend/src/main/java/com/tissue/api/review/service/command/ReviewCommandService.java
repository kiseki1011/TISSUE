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
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewCommandService {

	private final ReviewReader reviewReader;
	private final ReviewerReader reviewerReader;
	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final ReviewRepository reviewRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public SubmitReviewResponse submitReview(
		String workspaceCode,
		String issueKey,
		Long requesterMemberId,
		SubmitReviewRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewerWorkspaceMember = workspaceMemberReader.findWorkspaceMember(
			requesterMemberId,
			workspaceCode
		);

		IssueReviewer reviewer = reviewerReader.findByIssueKeyAndWorkspaceMemberId(
			issueKey,
			reviewerWorkspaceMember.getId()
		);

		issue.validateCanSubmitReview();

		Review review = reviewer.submitReview(
			request.status(),
			request.title(),
			request.content()
		);

		Review savedReview = reviewRepository.save(review);

		eventPublisher.publishEvent(
			ReviewSubmittedEvent.createEvent(issue, requesterMemberId, savedReview)
		);

		return SubmitReviewResponse.from(savedReview);
	}

	@Transactional
	public UpdateReviewResponse updateReview(
		String workspaceCode,
		Long reviewId,
		Long reviewerMemberId,
		UpdateReviewRequest request
	) {
		Review review = reviewReader.findReview(reviewId);
		// TODO: validation service 또는 authorization service를 따로 만들어서 처리
		// review.validateIsAuthor(reviewerWorkspaceMemberId);

		review.updateTitle(request.title());
		review.updateContent(request.content());

		return UpdateReviewResponse.from(review);
	}

}
