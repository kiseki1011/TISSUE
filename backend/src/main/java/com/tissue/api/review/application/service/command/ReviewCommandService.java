package com.tissue.api.review.application.service.command;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewCommandService {

	// private final ReviewReader reviewReader;
	// private final IssueFinder issueFinder;
	// private final WorkspaceMemberFinder workspaceMemberFinder;
	// private final ReviewRepository reviewRepository;
	// private final IssueReviewerRepository issueReviewerRepository;
	// private final ApplicationEventPublisher eventPublisher;

	// @Transactional
	// public ReviewResponse submitReview(
	// 	String workspaceCode,
	// 	String issueKey,
	// 	Long requesterMemberId,
	// 	SubmitReviewRequest request
	// ) {
	// 	Issue issue = issueReader.findIssue(issueKey, workspaceCode);
	//
	// 	WorkspaceMember reviewerWorkspaceMember = workspaceMemberReader.findWorkspaceMember(
	// 		requesterMemberId,
	// 		workspaceCode
	// 	);
	//
	// 	IssueReviewer reviewer = findReviewer(issueKey, reviewerWorkspaceMember);
	//
	// 	Review review = reviewer.submitReviewForCurrentRound(
	// 		request.status(),
	// 		request.title(),
	// 		request.content()
	// 	);
	//
	// 	Review savedReview = reviewRepository.save(review);
	//
	// 	eventPublisher.publishEvent(
	// 		ReviewSubmittedEvent.createEvent(issue, requesterMemberId, savedReview)
	// 	);
	//
	// 	return ReviewResponse.from(savedReview);
	// }
	//
	// @Transactional
	// public ReviewResponse updateReview(
	// 	String workspaceCode,
	// 	Long reviewId,
	// 	Long reviewerMemberId,
	// 	UpdateReviewRequest request
	// ) {
	// 	Review review = reviewReader.findReview(reviewId);
	// 	// TODO: validation service 또는 authorization service를 따로 만들어서 처리
	// 	// review.validateIsAuthor(reviewerWorkspaceMemberId);
	//
	// 	review.updateTitle(request.title());
	// 	review.updateContent(request.content());
	//
	// 	return ReviewResponse.from(review);
	// }
	//
	// private IssueReviewer findReviewer(String issueKey, WorkspaceMember reviewer) {
	// 	return issueReviewerRepository.findByIssueKeyAndReviewerId(issueKey, reviewer.getId())
	// 		.orElseThrow(() -> new InvalidOperationException(String.format(
	// 			"Must be a reviewer to create a review. issueKey: %s, workspaceMemberId: %d",
	// 			issueKey, reviewer.getId()))
	// 		);
	// }

}
