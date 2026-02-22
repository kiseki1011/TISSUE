package com.tissue.feature.organization.position.web.request;

import static com.tissue.feature.organization.policy.OrganizationConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.organization.policy.OrganizationConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.organization.policy.OrganizationConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.shared.enums.ColorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Size(max = DESCRIPTION_MAX_LENGTH) String description,

        @NotNull ColorType color) {

    public CreatePositionCommand toCommand() {
        return new CreatePositionCommand(name, description, color);
    }
}
