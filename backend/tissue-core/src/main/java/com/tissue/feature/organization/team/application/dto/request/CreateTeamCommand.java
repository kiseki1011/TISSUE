package com.tissue.feature.organization.team.application.dto.request;

import com.tissue.shared.enums.ColorType;

public record CreateTeamCommand(String name, String description, ColorType color) {}
