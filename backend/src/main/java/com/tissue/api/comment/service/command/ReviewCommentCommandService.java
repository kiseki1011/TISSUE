package com.tissue.api.comment.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.ReviewComment;
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
		Long memberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

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

		return ReviewCommentResponse.from(issue, savedComment);
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
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(workspaceMember);
		comment.updateContent(request.content());

		return ReviewCommentResponse.from(issue, comment);
	}

	@Transactional
	public ReviewCommentResponse deleteComment(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		Long commentId,
		Long memberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		ReviewComment comment = commentRepository.findByIdAndReview_IdAndReview_IssueKey(commentId, reviewId, issueKey)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(workspaceMember);
		comment.softDelete(memberId);

		return ReviewCommentResponse.from(issue, comment);
	}
}
