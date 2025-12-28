package com.tissue.comment.application.dto.in;

import lombok.Builder;

@Builder
public record AddCommentCommand(
        String workspaceKey,
        String projectKey,
        String issueKey,
        String content,
        Long parentCommentId,
        Long actorMemberId) {}
