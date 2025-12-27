package com.tissue.team.application.dto.request;

import com.tissue.common.enums.ColorType;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamCommand(
        String workspaceKey,
        Long teamId,
        JsonNullable<String> name,
        JsonNullable<String> description,
        JsonNullable<ColorType> color) {}
