package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issuetype.domain.FieldOption;
import org.jspecify.annotations.Nullable;

public record EnumOptionInfo(Long id, String displayName) {
    public @Nullable static EnumOptionInfo of(@Nullable FieldOption option) {
        if (option == null) {
            return null;
        }
        return new EnumOptionInfo(option.getId(), option.getName());
    }
}
