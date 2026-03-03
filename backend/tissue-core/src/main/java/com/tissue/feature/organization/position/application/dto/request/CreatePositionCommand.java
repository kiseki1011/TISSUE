package com.tissue.feature.organization.position.application.dto.request;

import com.tissue.shared.enums.ColorType;

public record CreatePositionCommand(String name, String description, ColorType color) {}
