package com.tissue.workspace.web.request;

import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkspaceInfoRequest(
        JsonNullable<@Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) @NotEmpty String> name,
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description) {

    public UpdateWorkspaceInfoCommand toCommand() {
        return new UpdateWorkspaceInfoCommand(name, description);
    }
}
