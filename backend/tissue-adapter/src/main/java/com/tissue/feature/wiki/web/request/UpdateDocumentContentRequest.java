package com.tissue.feature.wiki.web.request;

import static com.tissue.feature.wiki.domain.policy.WikiDocumentConstraintPolicy.CONTENT_MAX_LENGTH;
import static com.tissue.feature.wiki.domain.policy.WikiDocumentConstraintPolicy.EDIT_REASON_MAX_LENGTH;

import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateDocumentContentRequest(
        @NotBlank @Size(max = CONTENT_MAX_LENGTH) String content,

        @NotNull SemanticUpdateType versionUpdateType,

        @Nullable @Size(max = EDIT_REASON_MAX_LENGTH) String editReason) {

    public UpdateDocumentContentCommand toCommand() {
        return new UpdateDocumentContentCommand(content, versionUpdateType, editReason);
    }
}
