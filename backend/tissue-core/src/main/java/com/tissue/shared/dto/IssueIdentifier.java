package com.tissue.shared.dto;

public record IssueIdentifier(String projectKey, String issueKey) {

    /**
     * Builds an identifier from a globally-unique issueKey alone. projectKey is derived from the
     * issueKey prefix for display. Used by the {@code /api/v1/issues/...} URLs.
     */
    public static IssueIdentifier ofIssueKey(String issueKey) {
        int idx = issueKey.lastIndexOf("-");
        String projectKey = issueKey.substring(0, idx);
        return new IssueIdentifier(projectKey, issueKey);
    }
}
