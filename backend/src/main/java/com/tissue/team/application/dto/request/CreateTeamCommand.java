package com.tissue.team.application.dto.request;

import com.tissue.common.enums.ColorType;

public record CreateTeamCommand(String name, String description, ColorType color) {}
