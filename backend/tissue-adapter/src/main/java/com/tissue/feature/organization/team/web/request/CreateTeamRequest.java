package com.tissue.feature.organization.team.web.request;

import static com.tissue.feature.organization.domain.policy.OrganizationConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.organization.domain.policy.OrganizationConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.organization.domain.policy.OrganizationConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.shared.enums.ColorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Size(max = DESCRIPTION_MAX_LENGTH) String description,
        @NotNull ColorType color) {

    public CreateTeamCommand toCommand() {
        return new CreateTeamCommand(name, description, color);
    }
}
