package com.tissue.feature.issue.web.request;

import jakarta.validation.constraints.NotBlank;

public record AssignParentIssueRequest(@NotBlank String parentIssueKey) {}
