package com.tissue.comment.application.dto.out;

import java.time.Instant;

import com.tissue.comment.domain.Comment;

public record MyCommentResponse(
	Long commentId,
	String content,
	boolean isEdited,
	Instant createdAt,
	Instant lastUpdatedAt,
	String workspaceKey,
	String projectKey,
	String issueKey,
	String issueTitle
) {
	public static MyCommentResponse from(Comment comment) {
		return new MyCommentResponse(
			comment.getId(),
			comment.getContent(),
			comment.isEdited(),
			comment.getCreatedAt(),
			comment.getLastModifiedAt(),
			comment.getIssue().getWorkspaceKey(),
			comment.getIssue().getProjectKey(),
			comment.getIssue().getKey(),
			comment.getIssue().getTitle()
		);
	}
}
