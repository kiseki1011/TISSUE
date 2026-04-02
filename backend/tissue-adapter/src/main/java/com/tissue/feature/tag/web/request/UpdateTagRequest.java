package com.tissue.feature.tag.web.request;

import static com.tissue.feature.tag.domain.policy.TagConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.tag.application.dto.request.UpdateTagCommand;
import com.tissue.shared.enums.ColorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTagRequest(
        JsonNullable<@NotBlank String> name,
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,
        JsonNullable<ColorType> color) {

    public UpdateTagCommand toCommand() {
        return new UpdateTagCommand(name, description, color);
    }
}
