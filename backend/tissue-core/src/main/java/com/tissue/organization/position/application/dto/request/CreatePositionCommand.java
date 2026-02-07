package com.tissue.organization.position.application.dto.request;

import com.tissue.common.enums.ColorType;

public record CreatePositionCommand(String name, String description, ColorType color) {}
