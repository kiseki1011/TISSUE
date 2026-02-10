package com.tissue.organization.position.web.request;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.shared.enums.ColorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull ColorType color) {

    public CreatePositionCommand toCommand() {
        return new CreatePositionCommand(name, description, color);
    }
}
