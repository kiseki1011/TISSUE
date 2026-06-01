package com.tissue.feature.organization.team.adapter.web.request;

import static com.tissue.feature.organization.team.domain.policy.TeamConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.organization.team.application.dto.request.PatchTeamCommand;
import com.tissue.shared.enums.ColorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamRequest(
        JsonNullable<@NotBlank String> name,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,

        JsonNullable<ColorType> color) {

    public PatchTeamCommand toCommand() {
        return PatchTeamCommand.builder()
                .name(name)
                .description(description)
                .color(color)
                .build();
    }
}
