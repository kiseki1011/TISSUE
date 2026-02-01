package com.tissue.team.adapter.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.team.application.dto.request.UpdateTeamCommand;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamRequest(
        JsonNullable<@NotBlank @Size(max = 100) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdateTeamCommand toCommand(Long teamId, WorkspaceMemberContext actorContext) {
        return new UpdateTeamCommand(teamId, name, description, color, actorContext);
    }
}
