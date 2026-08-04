package com.tissue.feature.agent.model.adapter.web.request;

import static com.tissue.feature.agent.model.domain.policy.AiModelConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.agent.model.application.dto.request.PatchAiModelCommand;
import com.tissue.shared.enums.ColorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateAiModelRequest(
        JsonNullable<@NotBlank String> name,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,

        JsonNullable<ColorType> color) {

    public PatchAiModelCommand toCommand() {
        return PatchAiModelCommand.builder()
                .name(name)
                .description(description)
                .color(color)
                .build();
    }
}
