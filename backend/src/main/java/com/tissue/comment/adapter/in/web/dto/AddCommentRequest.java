package com.tissue.comment.adapter.in.web.dto;

import com.tissue.comment.application.dto.in.AddCommentCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record AddCommentRequest(
        @NotBlank @Size(max = 10000) String content,
        @Nullable Long parentCommentId) {

    public AddCommentCommand toCommand(String issueKey, ProjectMemberContext actorContext) {
        return AddCommentCommand.builder()
                .issueKey(issueKey)
                .content(content)
                .parentCommentId(parentCommentId)
                .actorContext(actorContext)
                .build();
    }
}
