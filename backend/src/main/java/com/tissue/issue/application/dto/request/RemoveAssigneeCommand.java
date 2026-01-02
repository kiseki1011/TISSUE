package com.tissue.issue.application.dto.request;

public record RemoveAssigneeCommand(String workspaceKey, String projectKey, String issueKey) {}
