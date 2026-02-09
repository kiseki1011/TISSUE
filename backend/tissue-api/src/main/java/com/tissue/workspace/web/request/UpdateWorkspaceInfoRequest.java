package com.tissue.workspace.web.request;

import com.tissue.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkspaceInfoRequest(
        JsonNullable<@Size(max = 100) @NotEmpty String> name,
        JsonNullable<@Size(max = 255) @NotEmpty String> description) {

    public UpdateWorkspaceInfoCommand toCommand() {
        return new UpdateWorkspaceInfoCommand(name, description);
    }
}
