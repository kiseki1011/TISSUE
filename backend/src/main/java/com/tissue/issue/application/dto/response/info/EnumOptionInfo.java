package com.tissue.issue.application.dto.response.info;

import com.tissue.issuetype.domain.EnumFieldOption;
import org.jspecify.annotations.Nullable;

public record EnumOptionInfo(Long id, String displayName) {

    public @Nullable static EnumOptionInfo of(@Nullable EnumFieldOption option) {
        if (option == null) {
            return null;
        }
        return new EnumOptionInfo(option.getId(), option.getName());
    }
}
