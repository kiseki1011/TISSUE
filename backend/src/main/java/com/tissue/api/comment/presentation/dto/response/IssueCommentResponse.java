package com.tissue.api.comment.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.comment.domain.IssueComment;
import com.tissue.api.comment.domain.enums.CommentStatus;

import lombok.Builder;

@Builder
public record IssueCommentResponse(
	Long id,
	String content,
	AuthorInfo author,
	LocalDateTime createdAt,
	boolean isEdited,
	CommentStatus status,
	List<IssueCommentResponse> childComments
) {
	public record AuthorInfo(
		Long workspaceMemberId,
		String nickname
	) {
	}

	public static IssueCommentResponse from(IssueComment comment) {
		return IssueCommentResponse.builder()
			.id(comment.getId())
			.content(comment.getContent())
			.author(new AuthorInfo(
				comment.getAuthor().getId(),
				comment.getAuthor().getNickname()
			))
			.createdAt(comment.getCreatedDate())
			.isEdited(comment.isEdited())
			.status(comment.getStatus())
			.childComments(comment.getChildComments().stream()
				.map(child -> from((IssueComment)child))
				.toList())
			.build();
	}
}
