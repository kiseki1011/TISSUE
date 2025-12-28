package com.tissue.comment.adapter.in.web.dto;

import com.tissue.comment.application.dto.in.AddCommentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record AddCommentRequest(
        @NotBlank @Size(max = 10000) String content, @Nullable Long parentCommentId) {

    public AddCommentCommand toCommand(
            String workspaceKey, String projectKey, String issueKey, Long currentMemberId) {
        return AddCommentCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .content(content)
                .parentCommentId(parentCommentId)
                .actorMemberId(currentMemberId)
                .build();
    }
}
