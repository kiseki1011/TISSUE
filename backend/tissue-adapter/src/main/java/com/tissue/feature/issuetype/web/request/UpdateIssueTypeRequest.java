package com.tissue.feature.issuetype.web.request;

import static com.tissue.feature.issuetype.domain.policy.IssueTypeConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateIssueTypeRequest(
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,
        JsonNullable<ColorType> color,
        JsonNullable<IconType> icon) {

    public PatchIssueTypeCommand toCommand() {
        return PatchIssueTypeCommand.builder()
                .description(description)
                .color(color)
                .icon(icon)
                .build();
    }
}
