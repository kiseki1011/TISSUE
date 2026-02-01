package com.tissue.comment.adapter.web.dto;

import com.tissue.comment.application.dto.request.AddCommentCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record AddCommentRequest(
    @NotBlank @Size(max = 10000) String content,
    @Nullable List<String> mentionedUsernames,
    @Nullable Long parentCommentId) {

    public AddCommentCommand toCommand(String issueKey, ProjectMemberContext actorContext) {
        return AddCommentCommand.builder()
                                .issueKey(issueKey)
                                .content(content)
                                .mentionedUsernames(
                                    mentionedUsernames != null ? mentionedUsernames : List.of())
                                .parentCommentId(parentCommentId)
                                .actorContext(actorContext)
                                .build();
    }
}
