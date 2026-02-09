package com.tissue.issue.application.dto.response.info;

import com.tissue.enums.ColorType;
import com.tissue.issuetype.domain.IssueType;

public record IssueTypeInfo(
        Long id,
        String displayName,
        ColorType color,
        // String icon
        boolean canUseStoryPoint) {
    public static IssueTypeInfo from(IssueType issueType) {
        return new IssueTypeInfo(
                issueType.getId(), issueType.getName(), issueType.getColor(), issueType.canUseStoryPoint());
    }
}
