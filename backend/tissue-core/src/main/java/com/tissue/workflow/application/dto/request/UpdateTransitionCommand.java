package com.tissue.workflow.application.dto.request;

import com.tissue.global.vo.Name;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateTransitionCommand(JsonNullable<Name> name, JsonNullable<String> description) {}
