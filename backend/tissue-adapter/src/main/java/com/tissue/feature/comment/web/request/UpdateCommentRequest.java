package com.tissue.feature.comment.web.request;

import com.tissue.feature.comment.application.dto.request.UpdateCommentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateCommentRequest(
        @NotBlank @Size(max = 10000) String content,
        @Size(max = 50) List<String> mentionedUsernames) {

    public UpdateCommentCommand toCommand() {
        return new UpdateCommentCommand(content, mentionedUsernames != null ? mentionedUsernames : List.of());
    }
}
