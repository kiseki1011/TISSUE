package com.tissue.api.comment.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.IssueComment;
import com.tissue.api.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.api.comment.domain.repository.CommentRepository;
import com.tissue.api.comment.exception.CommentNotFoundException;
import com.tissue.api.comment.presentation.dto.request.CreateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.response.IssueCommentResponse;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommentCommandService {

	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final CommentRepository commentRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueCommentResponse createComment(
		String workspaceCode,
		String issueKey,
		CreateIssueCommentRequest request,
		Long currentWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember currentWorkspaceMember = workspaceMemberReader.findWorkspaceMember(
			currentWorkspaceMemberId);

		IssueComment parentComment = null;
		if (request.hasParentComment()) {
			parentComment = (IssueComment)commentRepository.findById(request.parentCommentId())
				.orElseThrow(() -> new CommentNotFoundException(request.parentCommentId()));
		}

		// Todo: 아래로 리팩토링 가능
		// IssueComment parentComment = request.hasParentComment()
		// 	? findIssueComment(request.parentCommentId())
		// 	: null;

		IssueComment comment = IssueComment.builder()
			.content(request.content())
			.issue(issue)
			.parentComment(parentComment)
			.author(currentWorkspaceMember)
			.build();

		IssueComment savedComment = commentRepository.save(comment);

		eventPublisher.publishEvent(
			IssueCommentAddedEvent.createEvent(issue, savedComment, currentWorkspaceMemberId)
		);

		return IssueCommentResponse.from(comment);
	}

	@Transactional
	public IssueCommentResponse updateComment(
		String workspaceCode,
		String issueKey,
		Long commentId,
		UpdateIssueCommentRequest request,
		Long currentWorkspaceMemberId
	) {
		WorkspaceMember currentWorkspaceMember = workspaceMemberReader.findWorkspaceMember(
			currentWorkspaceMemberId);

		IssueComment comment = commentRepository.findByIdAndIssue_IssueKeyAndIssue_WorkspaceCode(commentId, issueKey,
				workspaceCode)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(currentWorkspaceMember);
		comment.updateContent(request.content());

		return IssueCommentResponse.from(comment);
	}

	@Transactional
	public void deleteComment(
		String workspaceCode,
		String issueKey,
		Long commentId,
		Long currentWorkspaceMemberId
	) {
		WorkspaceMember currentWorkspaceMember = workspaceMemberReader.findWorkspaceMember(
			currentWorkspaceMemberId);

		IssueComment comment = commentRepository.findByIdAndIssue_IssueKeyAndIssue_WorkspaceCode(commentId, issueKey,
				workspaceCode)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(currentWorkspaceMember);
		comment.softDelete(currentWorkspaceMemberId);
	}
}
