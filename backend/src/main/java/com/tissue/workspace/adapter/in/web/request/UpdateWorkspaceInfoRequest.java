package com.tissue.workspace.adapter.in.web.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkspaceInfoRequest(
        JsonNullable<@Size(max = 100) @NotEmpty String> name,
        JsonNullable<@Size(max = 255) @NotEmpty String> description) {

    public UpdateWorkspaceInfoCommand toCommand(WorkspaceMemberContext actorContext) {
        return new UpdateWorkspaceInfoCommand(name, description, actorContext);
    }
}
