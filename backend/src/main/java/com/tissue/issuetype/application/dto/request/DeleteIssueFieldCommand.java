package com.tissue.issuetype.application.dto.request;

import lombok.Builder;

@Builder
public record DeleteIssueFieldCommand(
        String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId) {}
