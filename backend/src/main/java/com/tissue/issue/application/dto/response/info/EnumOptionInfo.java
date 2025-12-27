package com.tissue.issue.application.dto.response.info;

import com.tissue.issuetype.domain.EnumFieldOption;

public record EnumOptionInfo(Long id, String displayName) {
    public static EnumOptionInfo of(EnumFieldOption option) {
        if (option == null) {
            return null;
        }
        return new EnumOptionInfo(option.getId(), option.getDisplayName());
    }
}
