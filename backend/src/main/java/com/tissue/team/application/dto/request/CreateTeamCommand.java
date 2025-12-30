package com.tissue.team.application.dto.request;

import com.tissue.common.enums.ColorType;

public record CreateTeamCommand(String workspaceKey, String name, String description, ColorType color) {}
