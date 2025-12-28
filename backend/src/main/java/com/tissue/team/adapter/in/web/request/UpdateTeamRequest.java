package com.tissue.team.adapter.in.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.team.application.dto.request.UpdateTeamCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamRequest(
        JsonNullable<@NotBlank @Size(max = 100) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {
    public UpdateTeamCommand toCommand(String workspaceKey, Long teamId) {
        return new UpdateTeamCommand(workspaceKey, teamId, name, description, color);
    }
}
