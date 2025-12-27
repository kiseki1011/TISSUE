package com.tissue.sprint.application.dto.request;

public record CompleteSprintCommand(String workspaceKey, String projectKey, Long sprintId) {}
