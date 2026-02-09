package com.tissue.organization.team.web.request;

import com.tissue.enums.ColorType;
import com.tissue.organization.team.application.dto.request.UpdateTeamCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamRequest(
        JsonNullable<@NotBlank @Size(max = 100) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdateTeamCommand toCommand() {
        return new UpdateTeamCommand(name, description, color);
    }
}
