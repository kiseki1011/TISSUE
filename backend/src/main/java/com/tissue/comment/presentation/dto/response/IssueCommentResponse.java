package com.tissue.comment.presentation.dto.response;

import com.tissue.comment.domain.IssueComment;

import lombok.Builder;

@Builder
public record IssueCommentResponse(
	String workspaceCode,
	String issueKey,
	Long commentId
) {
	public static IssueCommentResponse from(IssueComment comment) {
		return IssueCommentResponse.builder()
			.workspaceCode(comment.getIssue().getWorkspaceKey())
			.issueKey(comment.getIssue().getKey())
			.commentId(comment.getId())
			.build();
	}
}
