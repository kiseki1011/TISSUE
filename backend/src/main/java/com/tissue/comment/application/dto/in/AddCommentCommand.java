package com.tissue.comment.application.dto.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record AddCommentCommand(
        String issueKey, String content, @Nullable Long parentCommentId, ProjectMemberContext actorContext) {}
