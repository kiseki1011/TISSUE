package com.tissue.shared.dto;

public record IssueIdentifier(String workspaceKey, String projectKey, String issueKey) {

    public static IssueIdentifier of(String workspaceKey, String projectKey, String issueKey) {
        return new IssueIdentifier(workspaceKey, projectKey, issueKey);
    }

    public static IssueIdentifier of(String workspaceKey, String issueKey) {
        int idx = issueKey.lastIndexOf("-");
        String projectKey = issueKey.substring(0, idx);
        return new IssueIdentifier(workspaceKey, projectKey, issueKey);
    }
}
