package com.tissue.workspace.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateWorkspaceInfoCommand(JsonNullable<String> name, JsonNullable<String> description) {}
