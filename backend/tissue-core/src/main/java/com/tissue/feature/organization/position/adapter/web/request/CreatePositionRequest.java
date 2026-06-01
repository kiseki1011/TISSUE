package com.tissue.feature.organization.position.adapter.web.request;

import static com.tissue.feature.organization.position.domain.policy.PositionConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.organization.position.domain.policy.PositionConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.organization.position.domain.policy.PositionConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreatePositionRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,

        @NotNull ColorType color) {

    public CreatePositionCommand toCommand() {
        return CreatePositionCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .build();
    }
}
