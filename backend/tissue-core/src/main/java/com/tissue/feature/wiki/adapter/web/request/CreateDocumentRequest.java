package com.tissue.feature.wiki.adapter.web.request;

import static com.tissue.feature.wiki.domain.policy.WikiDocumentConstraintPolicy.CONTENT_MAX_LENGTH;
import static com.tissue.feature.wiki.domain.policy.WikiDocumentConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.wiki.domain.policy.WikiDocumentConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateDocumentRequest(
        @NotBlank @Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH)
        String title,

        @NotBlank @Size(max = CONTENT_MAX_LENGTH) String content,
        @Nullable Long parentDocumentId) {

    public DocumentCreateCommand toCommand() {
        return new DocumentCreateCommand(title, content, parentDocumentId);
    }
}
