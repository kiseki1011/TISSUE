package com.tissue.workflow.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.global.vo.Name;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateWorkflowCommand(
        JsonNullable<Name> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
