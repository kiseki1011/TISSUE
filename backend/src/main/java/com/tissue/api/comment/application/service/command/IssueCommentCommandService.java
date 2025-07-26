package com.tissue.api.comment.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.api.comment.domain.model.IssueComment;
import com.tissue.api.comment.exception.CommentNotFoundException;
import com.tissue.api.comment.infrastructure.repository.CommentRepository;
import com.tissue.api.comment.presentation.dto.request.CreateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.response.IssueCommentResponse;
import com.tissue.api.issue.application.service.reader.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommentCommandService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final CommentRepository commentRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueCommentResponse createComment(
		String workspaceCode,
		String issueKey,
		CreateIssueCommentRequest request,
		Long memberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

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

		eventPublisher.publishEvent(
			IssueCommentAddedEvent.createEvent(issue, savedComment, memberId)
		);

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
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		IssueComment comment = commentRepository.findByIdAndIssue_IssueKeyAndIssue_WorkspaceCode(
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
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		IssueComment comment = commentRepository.findByIdAndIssue_IssueKeyAndIssue_WorkspaceCode(
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
