package com.tissue.api.comment.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.comment.domain.IssueComment;
import com.tissue.api.comment.domain.enums.CommentStatus;

public record IssueCommentResponse(
	Long id,
	String content,
	AuthorInfo author,
	LocalDateTime createdAt,
	boolean isEdited,
	boolean isDeleted,
	List<IssueCommentResponse> childComments
) {
	public record AuthorInfo(
		Long workspaceMemberId,
		String nickname
	) {
	}

	public static IssueCommentResponse from(IssueComment comment) {
		return new IssueCommentResponse(
			comment.getId(),
			comment.getContent(),
			new AuthorInfo(
				comment.getAuthor().getId(),
				comment.getAuthor().getNickname()
			),
			comment.getCreatedDate(),
			comment.isEdited(),
			comment.getStatus() == CommentStatus.DELETED,
			comment.getChildComments().stream()
				.map(child -> from((IssueComment)child))
				.toList()
		);
	}
}
