package com.tissue.comment.application.dto.in;

public record DeleteCommentCommand(Long commentId, Long actorMemberId) {}
