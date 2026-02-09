package com.tissue.comment.web.request;

import com.tissue.comment.application.dto.request.CreateCommentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record AddCommentRequest(
        @NotBlank @Size(max = 10000) String content,
        @Nullable List<String> mentionedUsernames,
        @Nullable Long parentCommentId) {

    public CreateCommentCommand toCommand() {
        return CreateCommentCommand.builder()
                .content(content)
                .mentionedUsernames(mentionedUsernames != null ? mentionedUsernames : List.of())
                .parentCommentId(parentCommentId)
                .build();
    }
}
