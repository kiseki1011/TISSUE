package com.tissue.issue.application.dto.response;

import com.tissue.issue.domain.Issue;

public record IssueCreateResponse(String workspaceKey, String projectKey, String issueKey) {
    public static IssueCreateResponse from(Issue issue) {
        return new IssueCreateResponse(
                issue.getProjectKey(), issue.getWorkspaceKey(), issue.getKey());
    }
}
