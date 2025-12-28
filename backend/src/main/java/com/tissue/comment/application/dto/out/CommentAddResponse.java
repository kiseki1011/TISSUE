package com.tissue.comment.application.dto.out;

public record CommentAddResponse(String workspaceKey, String issueKey, Long commentId) {}
