package com.tissue.team.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record CreateTeamCommand(
        String name, String description, ColorType color, WorkspaceMemberContext actorContext) {}
