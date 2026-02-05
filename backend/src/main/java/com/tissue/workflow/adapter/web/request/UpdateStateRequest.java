package com.tissue.workflow.adapter.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.common.util.JsonNullables;
import com.tissue.global.vo.Name;
import com.tissue.workflow.application.dto.request.UpdateStateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateStateRequest(
        JsonNullable<@NotBlank @Size(max = 32) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdateStateCommand toCommand() {
        return UpdateStateCommand.builder()
                .name(JsonNullables.map(name, Name::of))
                .description(description)
                .color(color)
                .build();
    }
}
