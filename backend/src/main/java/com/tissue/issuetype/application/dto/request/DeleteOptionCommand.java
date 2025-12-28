package com.tissue.issuetype.application.dto.request;

import lombok.Builder;

@Builder
public record DeleteOptionCommand(
        String workspaceKey,
        String projectKey,
        Long issueTypeId,
        Long issueFieldId,
        Long optionId) {}
