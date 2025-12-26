package com.tissue.comment.application.dto.out;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.tissue.comment.domain.Comment;

public record CommentDetailResponse(
	Long commentId,
	String content,
	boolean isEdited,
	Instant createdAt,
	Instant lastUpdatedAt,
	CommentAuthorInfo author,
	List<CommentDetailResponse> replies
) {
	public static CommentDetailResponse from(Comment comment, List<CommentDetailResponse> replies) {
		return new CommentDetailResponse(
			comment.getId(),
			comment.getContent(),
			comment.isEdited(),
			comment.getCreatedAt(),
			comment.getLastModifiedAt(),
			CommentAuthorInfo.from(comment.getAuthor()),
			replies != null ? replies : new ArrayList<>()
		);
	}
}
