package com.tissue.feature.organization.position.adapter.web.request;

import static com.tissue.feature.organization.position.domain.policy.PositionConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.organization.position.application.dto.request.PatchPositionCommand;
import com.tissue.shared.enums.ColorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdatePositionRequest(
        JsonNullable<@NotBlank String> name,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,

        JsonNullable<ColorType> color) {

    public PatchPositionCommand toCommand() {
        return PatchPositionCommand.builder()
                .name(name)
                .description(description)
                .color(color)
                .build();
    }
}
