package com.tissue.feature.wiki.adapter.web.request;

import static com.tissue.feature.wiki.domain.policy.WikiTagConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.wiki.domain.policy.WikiTagConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.wiki.application.dto.request.AttachWikiTagCommand;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record AttachWikiTagRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable ColorType color) {

    public AttachWikiTagCommand toCommand() {
        return new AttachWikiTagCommand(Name.of(name), color);
    }
}
