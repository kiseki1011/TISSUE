package com.tissue.shared.dto;

public record IssueIdentifier(String workspaceKey, String projectKey, String issueKey) {
    public static IssueIdentifier of(String workspaceKey, String projectKey, String issueKey) {
        return new IssueIdentifier(workspaceKey, projectKey, issueKey);
    }
}
