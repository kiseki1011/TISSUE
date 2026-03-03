package com.tissue.feature.comment.application.dto.request;

import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateCommentCommand(
        String content,
        List<String> mentionedUsernames,
        @Nullable Long parentCommentId) {}
