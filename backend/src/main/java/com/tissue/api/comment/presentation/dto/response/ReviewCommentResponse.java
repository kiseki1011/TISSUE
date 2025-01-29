package com.tissue.api.comment.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.comment.domain.ReviewComment;
import com.tissue.api.comment.domain.enums.CommentStatus;

import lombok.Builder;

@Builder
public record ReviewCommentResponse(
	Long id,
	String content,
	AuthorInfo author,
	LocalDateTime createdAt,
	boolean isEdited,
	CommentStatus status,
	List<ReviewCommentResponse> childComments
) {
	public record AuthorInfo(
		Long workspaceMemberId,
		String nickname
	) {
	}

	public static ReviewCommentResponse from(ReviewComment comment) {
		return ReviewCommentResponse.builder()
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
				.map(child -> from((ReviewComment)child))
				.toList())
			.build();
	}
}
