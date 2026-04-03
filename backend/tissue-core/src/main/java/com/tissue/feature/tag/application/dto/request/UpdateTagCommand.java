package com.tissue.feature.tag.application.dto.request;

import com.tissue.shared.enums.ColorType;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTagCommand(
        JsonNullable<String> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
