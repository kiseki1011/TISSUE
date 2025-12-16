package com.tissue.comment.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.comment.domain.model.IssueComment;
import com.tissue.comment.exception.CommentNotFoundException;
import com.tissue.comment.infrastructure.repository.CommentRepository;
import com.tissue.comment.presentation.dto.request.CreateIssueCommentRequest;
import com.tissue.comment.presentation.dto.request.UpdateIssueCommentRequest;
import com.tissue.comment.presentation.dto.response.IssueCommentResponse;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommentCommandService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final CommentRepository commentRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueCommentResponse createComment(
		String workspaceCode,
		String issueKey,
		CreateIssueCommentRequest request,
		Long memberId
	) {
		Issue issue = issueFinder.findBy(issueKey, workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(memberId, workspaceCode);

		IssueComment parentComment = request.hasParentComment()
			? (IssueComment)commentRepository.findById(request.parentCommentId())
			.orElseThrow(() -> new CommentNotFoundException(request.parentCommentId()))
			: null;

		IssueComment comment = IssueComment.builder()
			.content(request.content())
			.issue(issue)
			.parentComment(parentComment)
			.author(workspaceMember)
			.build();

		IssueComment savedComment = commentRepository.save(comment);

		return IssueCommentResponse.from(savedComment);
	}

	@Transactional
	public IssueCommentResponse updateComment(
		String workspaceCode,
		String issueKey,
		Long commentId,
		UpdateIssueCommentRequest request,
		Long memberId
	) {
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(memberId, workspaceCode);

		IssueComment comment = commentRepository.findByIdAndIssue_KeyAndIssue_WorkspaceKey(
				commentId,
				issueKey,
				workspaceCode
			)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(workspaceMember);
		comment.updateContent(request.content());

		return IssueCommentResponse.from(comment);
	}

	@Transactional
	public IssueCommentResponse deleteComment(
		String workspaceCode,
		String issueKey,
		Long commentId,
		Long memberId
	) {
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(memberId, workspaceCode);

		IssueComment comment = commentRepository.findByIdAndIssue_KeyAndIssue_WorkspaceKey(
				commentId,
				issueKey,
				workspaceCode
			)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(workspaceMember);
		comment.softDelete(memberId);

		return IssueCommentResponse.from(comment);
	}
}
