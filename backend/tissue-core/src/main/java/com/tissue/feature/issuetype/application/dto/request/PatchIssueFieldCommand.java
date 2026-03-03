package com.tissue.feature.issuetype.application.dto.request;

import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchIssueFieldCommand(JsonNullable<String> description, JsonNullable<Boolean> required) {}
