package com.tissue.api.comment.presentation.dto.response;

import com.tissue.api.comment.domain.model.IssueComment;

import lombok.Builder;

@Builder
public record IssueCommentResponse(
	String workspaceCode,
	String issueKey,
	Long commentId
) {
	public static IssueCommentResponse from(IssueComment comment) {
		return IssueCommentResponse.builder()
			.workspaceCode(comment.getIssue().getWorkspaceCode())
			.issueKey(comment.getIssue().getKey())
			.commentId(comment.getId())
			.build();
	}
}
