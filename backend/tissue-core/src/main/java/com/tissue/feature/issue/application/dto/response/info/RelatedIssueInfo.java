package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssuePriority;

public record RelatedIssueInfo(
        String issueKey, String title, IssueTypeInfo issueType, StateInfo currentState, IssuePriority priority) {

    public static RelatedIssueInfo from(Issue issue) {
        return new RelatedIssueInfo(
                issue.getKey(),
                issue.getTitle(),
                IssueTypeInfo.from(issue.getIssueType()),
                StateInfo.from(issue.getCurrentState()),
                issue.getPriority());
    }
}
