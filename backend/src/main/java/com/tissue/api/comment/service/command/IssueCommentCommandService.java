package com.tissue.api.comment.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.comment.domain.IssueComment;
import com.tissue.api.comment.domain.repository.CommentRepository;
import com.tissue.api.comment.exception.CommentNotFoundException;
import com.tissue.api.comment.presentation.dto.request.CreateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.response.IssueCommentResponse;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommentCommandService {

	private final CommentRepository commentRepository;
	private final IssueRepository issueRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional
	public IssueCommentResponse createComment(
		String workspaceCode,
		String issueKey,
		CreateIssueCommentRequest request,
		Long currentWorkspaceMemberId
	) {
		// Todo: IssueQueryService 구현 후 재사용하는 방식으로 리팩토링
		Issue issue = issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, workspaceCode));

		WorkspaceMember author = workspaceMemberRepository.findById(currentWorkspaceMemberId)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(currentWorkspaceMemberId, workspaceCode));

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
			.author(author)
			.build();

		commentRepository.save(comment);

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
		WorkspaceMember currentWorkspaceMember = workspaceMemberRepository.findById(currentWorkspaceMemberId)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(currentWorkspaceMemberId, workspaceCode));

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
		WorkspaceMember currentWorkspaceMember = workspaceMemberRepository.findById(currentWorkspaceMemberId)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(currentWorkspaceMemberId, workspaceCode));

		IssueComment comment = commentRepository.findByIdAndIssue_IssueKeyAndIssue_WorkspaceCode(commentId, issueKey,
				workspaceCode)
			.orElseThrow(() -> new CommentNotFoundException(commentId));

		comment.validateCanEdit(currentWorkspaceMember);
		comment.softDelete(currentWorkspaceMemberId);
	}
}
