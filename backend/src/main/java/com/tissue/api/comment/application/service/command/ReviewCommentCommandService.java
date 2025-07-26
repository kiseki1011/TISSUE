package com.tissue.api.comment.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.model.ReviewComment;
import com.tissue.api.comment.exception.CommentNotFoundException;
import com.tissue.api.comment.infrastructure.repository.CommentRepository;
import com.tissue.api.comment.presentation.dto.request.CreateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateReviewCommentRequest;
import com.tissue.api.comment.presentation.dto.response.ReviewCommentResponse;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.application.service.reader.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.review.domain.model.Review;
import com.tissue.api.review.infrastructure.repository.ReviewRepository;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewCommentCommandService {

	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueFinder issueFinder;
	private final CommentRepository commentRepository;
	private final ReviewRepository reviewRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public ReviewCommentResponse createComment(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		CreateReviewCommentRequest request,
		Long memberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);

		// Todo: ReviewNotFoundException을 만들까? 근데 issueKey, workspaceCode 정보가 필요할까?
		Review review = reviewRepository.findByIdAndIssueKeyAndWorkspaceCode(reviewId, issueKey, workspaceCode)
			.orElseThrow(
				() -> new ResourceNotFoundException(String.format(
					"Review was not found with review id: %d, issue key: %s, workspace code: %s",
					reviewId, issueKey, workspaceCode))
			);

		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		ReviewComment parentComment = request.hasParentComment()
			? (ReviewComment)commentRepository.findById(request.parentCommentId())
			.orElseThrow(() -> new CommentNotFoundException(request.parentCommentId()))
			: null;

		ReviewComment comment = ReviewComment.builder()
			.content(request.content())
			.review(review)
			.parentComment(parentComment)
			.author(workspaceMember)
			.build();

		ReviewComment savedComment = commentRepository.save(comment);

		// TODO: 리뷰 댓글 달리는 것도 알림으로 알려줘야 할까?
		// TODO: 만약 알림을 알린다면, 해당 리뷰의 제목을 알림 내용에 포함하는게 좋을까?
		// eventPublisher.publishEvent(
		// 	ReviewCommentAddedEvent.createEvent(issue, review, savedComment, currentWorkspaceMemberId)
		// );

		return ReviewCommentResponse.from(savedComment);
	}

	@Transactional
	public ReviewCommentResponse updateComment(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		Long commentId,
		UpdateReviewCommentRequest request,
		Long memberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(workspaceMember);
		comment.updateContent(request.content());

		return ReviewCommentResponse.from(comment);
	}

	@Transactional
	public ReviewCommentResponse deleteComment(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		Long commentId,
		Long memberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(workspaceMember);
		comment.softDelete(memberId);

		return ReviewCommentResponse.from(comment);
	}
}
