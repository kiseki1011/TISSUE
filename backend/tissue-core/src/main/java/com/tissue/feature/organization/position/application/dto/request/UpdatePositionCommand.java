package com.tissue.feature.organization.position.application.dto.request;

import com.tissue.shared.enums.ColorType;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdatePositionCommand(
        JsonNullable<String> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
