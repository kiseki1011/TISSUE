package com.tissue.feature.issue.adapter.web.request;

import jakarta.validation.constraints.NotBlank;

public record AssignParentIssueRequest(@NotBlank String parentIssueKey) {}
