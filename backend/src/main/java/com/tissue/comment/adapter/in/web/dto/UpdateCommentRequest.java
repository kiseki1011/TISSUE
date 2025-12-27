package com.tissue.comment.adapter.in.web.dto;

import com.tissue.comment.application.dto.in.UpdateCommentCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
	@NotBlank
	@Size(max = 10000)
	String content
) {
	public UpdateCommentCommand toCommand(Long commentId, Long currentMemberId) {
		return new UpdateCommentCommand(commentId, content, currentMemberId);
	}
}
