package com.tissue.shared.dto;

import org.jspecify.annotations.Nullable;

public record IssueIdentifier(String workspaceKey, @Nullable String projectKey, String issueKey) {

    public static IssueIdentifier of(String workspaceKey, String projectKey, String issueKey) {
        return new IssueIdentifier(workspaceKey, projectKey, issueKey);
    }

    public static IssueIdentifier of(String workspaceKey, String issueKey) {
        return new IssueIdentifier(workspaceKey, null, issueKey);
    }
}
