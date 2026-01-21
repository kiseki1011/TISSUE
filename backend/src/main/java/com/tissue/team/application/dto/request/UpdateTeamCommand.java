package com.tissue.team.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamCommand(
        Long teamId,
        JsonNullable<String> name,
        JsonNullable<String> description,
        JsonNullable<ColorType> color,
        WorkspaceMemberContext actorContext) {}
