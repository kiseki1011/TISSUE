package com.tissue.feature.tag.adapter.web.request;

import static com.tissue.feature.tag.domain.policy.TagConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.tag.domain.policy.TagConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.tag.domain.policy.TagConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateTagRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
        @NotNull ColorType color) {

    public CreateTagCommand toCommand() {
        return CreateTagCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .build();
    }
}
