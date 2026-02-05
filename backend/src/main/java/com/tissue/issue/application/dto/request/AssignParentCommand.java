package com.tissue.issue.application.dto.request;

import lombok.Builder;

@Builder
public record AssignParentCommand(String parentIssueKey) {}
