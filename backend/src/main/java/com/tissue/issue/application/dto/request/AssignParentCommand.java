package com.tissue.issue.application.dto.request;

public record AssignParentCommand(
        String workspaceKey, String projectKey, String issueKey, String parentProjectKey, String parentIssueKey) {}
