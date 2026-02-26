package com.tissue.feature.issuetype.application.dto.request;

import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchIssueTypeCommand(
        JsonNullable<String> description, JsonNullable<ColorType> color, JsonNullable<IconType> icon) {}
