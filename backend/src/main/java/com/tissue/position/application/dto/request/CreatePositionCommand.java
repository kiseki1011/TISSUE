package com.tissue.position.application.dto.request;

import com.tissue.common.enums.ColorType;

public record CreatePositionCommand(
        String workspaceKey, String name, String description, ColorType color) {}
