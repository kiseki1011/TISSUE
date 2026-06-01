package com.tissue.feature.issuetype.adapter.web.request;

import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateIssueFieldRequest(
        JsonNullable<@NotBlank String> name,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,

        JsonNullable<Boolean> required) {

    public PatchIssueFieldCommand toCommand() {
        return PatchIssueFieldCommand.builder()
                .name(name)
                .description(description)
                .required(required)
                .build();
    }
}
