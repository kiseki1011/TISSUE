package com.tissue.issuetype.application.dto.request;

import com.tissue.common.enums.ColorType;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchIssueTypeCommand(JsonNullable<String> description, JsonNullable<ColorType> color) {}
