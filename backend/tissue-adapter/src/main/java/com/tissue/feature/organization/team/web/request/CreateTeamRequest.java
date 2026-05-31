package com.tissue.feature.organization.team.web.request;

import static com.tissue.feature.organization.team.domain.policy.TeamConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.organization.team.domain.policy.TeamConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.organization.team.domain.policy.TeamConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateTeamRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,

        @NotNull ColorType color) {

    public CreateTeamCommand toCommand() {
        return CreateTeamCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .build();
    }
}
