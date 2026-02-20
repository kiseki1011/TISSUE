package com.tissue.feature.organization.position.web.request;

import static com.tissue.feature.organization.domain.policy.OrganizationConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.organization.domain.policy.OrganizationConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.organization.domain.policy.OrganizationConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.organization.position.application.dto.request.UpdatePositionCommand;
import com.tissue.shared.enums.ColorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdatePositionRequest(
        JsonNullable<@NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) String> name,
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdatePositionCommand toCommand() {
        return new UpdatePositionCommand(name, description, color);
    }
}
