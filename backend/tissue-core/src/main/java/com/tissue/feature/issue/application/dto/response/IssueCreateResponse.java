package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.domain.Issue;

public record IssueCreateResponse(String issueKey) {
    public static IssueCreateResponse from(Issue issue) {
        return new IssueCreateResponse(issue.getKey());
    }
}
