package com.tissue.issuetype.web.request;

import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.shared.enums.ColorType;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateIssueTypeRequest(JsonNullable<@Size(max = 255) String> description, JsonNullable<ColorType> color) {

    public PatchIssueTypeCommand toCommand() {
        return PatchIssueTypeCommand.builder()
                .description(description)
                .color(color)
                .build();
    }
}
