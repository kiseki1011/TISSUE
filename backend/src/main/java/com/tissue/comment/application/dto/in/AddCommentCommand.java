package com.tissue.comment.application.dto.in;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record AddCommentCommand(
        String workspaceKey,
        String projectKey,
        String issueKey,
        String content,
        @Nullable Long parentCommentId,
        Long actorMemberId) {}
