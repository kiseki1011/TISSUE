package com.tissue.position.adapter.in.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.position.application.dto.request.UpdatePositionCommand;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdatePositionRequest(
        JsonNullable<@NotBlank @Size(max = 100) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdatePositionCommand toCommand(Long positionId, WorkspaceMemberContext actorContext) {
        return new UpdatePositionCommand(positionId, name, description, color, actorContext);
    }
}
