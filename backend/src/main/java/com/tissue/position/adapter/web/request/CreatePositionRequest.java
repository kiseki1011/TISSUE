package com.tissue.position.adapter.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.position.application.dto.request.CreatePositionCommand;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 255) String description,
    @NotNull ColorType color) {

    public CreatePositionCommand toCommand(WorkspaceMemberContext actorContext) {
        return new CreatePositionCommand(name, description, color, actorContext);
    }
}
