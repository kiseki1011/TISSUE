package com.tissue.feature.organization.team.web.request;

import static com.tissue.feature.organization.policy.OrganizationConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.organization.policy.OrganizationConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.organization.policy.OrganizationConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.organization.team.application.dto.request.UpdateTeamCommand;
import com.tissue.shared.enums.ColorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamRequest(
        @Schema(minLength = NAME_MIN_LENGTH, maxLength = NAME_MAX_LENGTH)
        JsonNullable<@NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) String> name,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,

        JsonNullable<@NotNull ColorType> color) {

    public UpdateTeamCommand toCommand() {
        return new UpdateTeamCommand(name, description, color);
    }
}
