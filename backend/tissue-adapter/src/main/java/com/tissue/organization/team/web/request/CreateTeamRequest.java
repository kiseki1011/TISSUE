package com.tissue.organization.team.web.request;

import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.shared.enums.ColorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull ColorType color) {

    public CreateTeamCommand toCommand() {
        return new CreateTeamCommand(name, description, color);
    }
}
