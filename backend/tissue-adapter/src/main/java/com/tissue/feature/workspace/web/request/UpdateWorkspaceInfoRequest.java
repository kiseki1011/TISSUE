package com.tissue.feature.workspace.web.request;

import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkspaceInfoRequest(
        @Schema(minLength = NAME_MIN_LENGTH, maxLength = NAME_MAX_LENGTH)
        JsonNullable<@Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) @NotBlank String> name,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description) {

    public UpdateWorkspaceInfoCommand toCommand() {
        return new UpdateWorkspaceInfoCommand(name, description);
    }
}
