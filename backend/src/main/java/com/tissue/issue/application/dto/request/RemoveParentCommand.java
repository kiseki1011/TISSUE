package com.tissue.issue.application.dto.request;

public record RemoveParentCommand(String workspaceKey, String projectKey, String issueKey, Long actorMemberId) {}
