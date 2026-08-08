package com.tissue.feature.agent.model.application.dto.request;

import com.tissue.shared.enums.ColorType;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchAiModelCommand(
        JsonNullable<String> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
