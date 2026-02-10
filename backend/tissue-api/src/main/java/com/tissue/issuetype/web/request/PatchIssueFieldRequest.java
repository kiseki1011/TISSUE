package com.tissue.issuetype.web.request;

import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchIssueFieldRequest(
        JsonNullable<@Size(max = 255) String> description, JsonNullable<Boolean> required) {

    public PatchIssueFieldCommand toCommand() {
        return PatchIssueFieldCommand.builder()
                .description(description)
                .required(required)
                .build();
    }
}
