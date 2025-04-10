package com.tissue.api.comment.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.ReviewComment;
import com.tissue.api.comment.domain.event.ReviewCommentAddedEvent;
import com.tissue.api.comment.domain.repository.CommentRepository;
import com.tissue.api.comment.exception.CommentNotFoundException;
import com.tissue.api.comment.presentation.dto.request.CreateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.response.ReviewCommentResponse;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewCommentCommandService {

	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueReader issueReader;
	private final CommentRepository commentRepository;
	private final ReviewRepository reviewRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public ReviewCommentResponse createComment(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		CreateReviewCommentRequest request,
		Long currentWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		// Todo: ReviewNotFoundException을 만들까? 근데 issueKey, workspaceCode 정보가 필요할까?
		Review review = reviewRepository.findByIdAndIssueKeyAndWorkspaceCode(reviewId, issueKey, workspaceCode)
			.orElseThrow(
				() -> new ResourceNotFoundException(String.format(
					"Review was not found with review id: %d, issue key: %s, workspace code: %s",
					reviewId, issueKey, workspaceCode))
			);

		WorkspaceMember currentWorkspaceMember = workspaceMemberReader.findWorkspaceMember(currentWorkspaceMemberId);

		ReviewComment parentComment = null;
		if (request.hasParentComment()) {
			parentComment = (ReviewComment)commentRepository.findById(request.parentCommentId())
				.orElseThrow(() -> new CommentNotFoundException(request.parentCommentId()));
		}

		ReviewComment comment = ReviewComment.builder()
			.content(request.content())
			.review(review)
			.parentComment(parentComment)
			.author(currentWorkspaceMember)
			.build();

		ReviewComment savedComment = commentRepository.save(comment);

		eventPublisher.publishEvent(
			ReviewCommentAddedEvent.createEvent(issue, review, savedComment, currentWorkspaceMemberId)
		);

		return ReviewCommentResponse.from(comment);
	}

	@Transactional
	public ReviewCommentResponse updateComment(
		String issueKey,
		Long reviewId,
		Long commentId,
		UpdateReviewCommentRequest request,
		Long currentWorkspaceMemberId
	) {
		WorkspaceMember currentWorkspaceMember = workspaceMemberReader.findWorkspaceMember(
			currentWorkspaceMemberId);

		ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(currentWorkspaceMember);
		comment.updateContent(request.content());

		return ReviewCommentResponse.from(comment);
	}

	@Transactional
	public void deleteComment(
		String issueKey,
		Long reviewId,
		Long commentId,
		Long currentWorkspaceMemberId
	) {
		WorkspaceMember currentWorkspaceMember = workspaceMemberReader.findWorkspaceMember(
			currentWorkspaceMemberId);

		ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(currentWorkspaceMember);
		comment.softDelete(currentWorkspaceMemberId);
	}
}
