package com.tissue.position.adapter.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.position.application.dto.request.UpdatePositionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdatePositionRequest(
        JsonNullable<@NotBlank @Size(max = 100) String> name,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<@NotNull ColorType> color) {

    public UpdatePositionCommand toCommand() {
        return new UpdatePositionCommand(name, description, color);
    }
}
