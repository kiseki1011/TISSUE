package com.tissue.feature.issuetype.application.dto.response;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;

public record IssueTypeSummary(
        Long id,
        String name,
        String description,
        IconType icon,
        ColorType color,
        IssueHierarchy hierarchy,
        Long workflowId,
        String workflowName,
        boolean systemProvided) {

    public static IssueTypeSummary from(IssueType issueType) {
        return new IssueTypeSummary(
                issueType.getId(),
                issueType.getName(),
                issueType.getDescription(),
                issueType.getIcon(),
                issueType.getColor(),
                issueType.getIssueHierarchy(),
                issueType.getWorkflow().getId(),
                issueType.getWorkflow().getName(),
                issueType.isSystemProvided());
    }
}
