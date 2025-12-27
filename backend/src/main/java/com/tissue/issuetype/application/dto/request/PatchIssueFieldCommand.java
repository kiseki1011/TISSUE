package com.tissue.issuetype.application.dto.request;

import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchIssueFieldCommand(
        String workspaceKey,
        String projectKey,
        Long issueTypeId,
        Long issueFieldId,
        JsonNullable<String> description,
        JsonNullable<Boolean> required) {}
