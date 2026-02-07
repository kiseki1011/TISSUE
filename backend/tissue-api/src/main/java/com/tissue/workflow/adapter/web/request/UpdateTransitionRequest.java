package com.tissue.workflow.adapter.web.request;

import com.tissue.common.util.JsonNullables;
import com.tissue.global.vo.Name;
import com.tissue.workflow.application.dto.request.UpdateTransitionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTransitionRequest(
        JsonNullable<@NotBlank @Size(max = 32) String> name, JsonNullable<@Size(max = 255) String> description) {

    public UpdateTransitionCommand toCommand() {
        return UpdateTransitionCommand.builder()
                .name(JsonNullables.map(name, Name::of))
                .description(description)
                .build();
    }
}
