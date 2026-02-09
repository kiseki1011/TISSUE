package com.tissue.organization.position.application.dto.request;

import com.tissue.enums.ColorType;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdatePositionCommand(
        JsonNullable<String> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
