package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;

public record IssueTypeInfo(Long id, String displayName, ColorType color, IconType icon, boolean canUseStoryPoint) {
    public static IssueTypeInfo from(IssueType issueType) {
        return new IssueTypeInfo(
                issueType.getId(),
                issueType.getName(),
                issueType.getColor(),
                issueType.getIcon(),
                issueType.canUseStoryPoint());
    }
}
