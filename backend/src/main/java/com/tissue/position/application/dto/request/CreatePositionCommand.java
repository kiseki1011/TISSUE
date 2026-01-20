package com.tissue.position.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record CreatePositionCommand(
        String name, String description, ColorType color, WorkspaceMemberContext actorContext) {}
