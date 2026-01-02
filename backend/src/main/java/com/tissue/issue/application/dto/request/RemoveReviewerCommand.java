package com.tissue.issue.application.dto.request;

import lombok.Builder;

@Builder
public record RemoveReviewerCommand(String workspaceKey, String projectKey, String issueKey, Long targetMemberId) {}
