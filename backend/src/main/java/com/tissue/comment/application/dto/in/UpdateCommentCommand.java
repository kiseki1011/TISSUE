package com.tissue.comment.application.dto.in;

public record UpdateCommentCommand(
	Long commentId,
	String content,
	Long actorMemberId
) {
}
