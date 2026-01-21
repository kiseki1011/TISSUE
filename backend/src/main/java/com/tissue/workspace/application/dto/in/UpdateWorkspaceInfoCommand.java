package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkspaceInfoCommand(
        JsonNullable<String> name, JsonNullable<String> description, WorkspaceMemberContext actorContext) {}
