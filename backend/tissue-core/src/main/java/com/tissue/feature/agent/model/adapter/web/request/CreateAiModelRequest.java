package com.tissue.feature.agent.model.adapter.web.request;

import static com.tissue.feature.agent.model.domain.policy.AiModelConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.agent.model.domain.policy.AiModelConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.agent.model.domain.policy.AiModelConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.agent.model.application.dto.request.CreateAiModelCommand;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateAiModelRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,

        @NotNull ColorType color) {

    public CreateAiModelCommand toCommand() {
        return CreateAiModelCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .build();
    }
}
