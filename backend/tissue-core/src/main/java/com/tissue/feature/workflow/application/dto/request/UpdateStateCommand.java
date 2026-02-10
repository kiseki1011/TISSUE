package com.tissue.feature.workflow.application.dto.request;

import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateStateCommand(
        JsonNullable<Name> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
