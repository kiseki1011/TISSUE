package com.tissue.feature.issue.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record RemoveIssueRelationRequest(
        @NotBlank String targetProjectKey, @NotBlank String targetIssueKey) {}
