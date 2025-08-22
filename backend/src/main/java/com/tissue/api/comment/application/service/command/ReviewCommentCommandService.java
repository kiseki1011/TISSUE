package com.tissue.api.comment.application.service.command;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewCommentCommandService {

	// private final WorkspaceMemberFinder workspaceMemberFinder;
	// private final IssueFinder issueFinder;
	// private final CommentRepository commentRepository;
	// private final ReviewRepository reviewRepository;
	// private final ApplicationEventPublisher eventPublisher;

	// @Transactional
	// public ReviewCommentResponse createComment(
	// 	String workspaceKey,
	// 	String issueKey,
	// 	Long reviewId,
	// 	CreateReviewCommentRequest request,
	// 	Long memberId
	// ) {
	// 	Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
	//
	// 	// Todo: ReviewNotFoundException을 만들까? 근데 issueKey, workspaceKey 정보가 필요할까?
	// 	Review review = reviewRepository.findByIdAndIssueKeyAndWorkspaceCode(reviewId, issueKey, workspaceKey)
	// 		.orElseThrow(
	// 			() -> new ResourceNotFoundException(String.format(
	// 				"Review was not found with review id: %d, issue key: %s, workspace code: %s",
	// 				reviewId, issueKey, workspaceKey))
	// 		);
	//
	// 	WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);
	//
	// 	ReviewComment parentComment = request.hasParentComment()
	// 		? (ReviewComment)commentRepository.findById(request.parentCommentId())
	// 		.orElseThrow(() -> new CommentNotFoundException(request.parentCommentId()))
	// 		: null;
	//
	// 	ReviewComment comment = ReviewComment.builder()
	// 		.content(request.content())
	// 		.review(review)
	// 		.parentComment(parentComment)
	// 		.author(workspaceMember)
	// 		.build();
	//
	// 	ReviewComment savedComment = commentRepository.save(comment);
	//
	// 	return ReviewCommentResponse.from(savedComment);
	// }

	// @Transactional
	// public ReviewCommentResponse updateComment(
	// 	String workspaceKey,
	// 	String issueKey,
	// 	Long reviewId,
	// 	Long commentId,
	// 	UpdateReviewCommentRequest request,
	// 	Long memberId
	// ) {
	// 	Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
	// 	WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);
	//
	// 	ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
	// 		.orElseThrow(() -> new CommentNotFoundException(commentId));
	//
	// 	comment.validateCanEdit(workspaceMember);
	// 	comment.updateContent(request.content());
	//
	// 	return ReviewCommentResponse.from(comment);
	// }

	// @Transactional
	// public ReviewCommentResponse deleteComment(
	// 	String workspaceKey,
	// 	String issueKey,
	// 	Long reviewId,
	// 	Long commentId,
	// 	Long memberId
	// ) {
	// 	Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
	// 	WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);
	//
	// 	ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
	// 		.orElseThrow(() -> new CommentNotFoundException(commentId));
	//
	// 	comment.validateCanEdit(workspaceMember);
	// 	comment.softDelete(memberId);
	//
	// 	return ReviewCommentResponse.from(comment);
	// }
}
