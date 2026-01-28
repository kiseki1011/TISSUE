package com.tissue.comment.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record AddCommentCommand(
        String issueKey,
        String content,
        List<String> mentionedUsernames,
        @Nullable Long parentCommentId,
        ProjectMemberContext actorContext) {}
