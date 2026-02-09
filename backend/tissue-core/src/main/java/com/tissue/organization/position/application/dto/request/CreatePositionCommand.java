package com.tissue.organization.position.application.dto.request;

import com.tissue.enums.ColorType;

public record CreatePositionCommand(String name, String description, ColorType color) {}
