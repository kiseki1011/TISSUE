package com.tissue.team.adapter.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.team.application.dto.request.CreateTeamCommand;
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
