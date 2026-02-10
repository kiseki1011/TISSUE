package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssuePriority;

public record RelatedIssueInfo(String issueKey, String title, String stateDisplayLabel, IssuePriority priority) {
    public static RelatedIssueInfo from(Issue issue) {
        return new RelatedIssueInfo(
                issue.getKey(), issue.getTitle(), issue.getCurrentState().getDisplayName(), issue.getPriority());
    }
}
