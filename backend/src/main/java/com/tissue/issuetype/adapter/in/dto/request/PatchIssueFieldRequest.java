package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.issuetype.application.dto.request.PatchIssueFieldCommand;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchIssueFieldRequest(
        JsonNullable<@Size(max = 255) String> description, JsonNullable<Boolean> required) {
    public PatchIssueFieldCommand toCommand(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId) {
        return PatchIssueFieldCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .description(description)
                .required(required)
                .build();
    }
}
