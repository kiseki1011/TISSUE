package com.tissue.feature.issuetype.web.request;

import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchIssueFieldRequest(
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description, JsonNullable<Boolean> required) {

    public PatchIssueFieldCommand toCommand() {
        return PatchIssueFieldCommand.builder()
                .description(description)
                .required(required)
                .build();
    }
}
