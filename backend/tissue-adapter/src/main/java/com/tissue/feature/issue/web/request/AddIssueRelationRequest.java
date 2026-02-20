package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddIssueRelationRequest(
        @NotBlank String targetProjectKey,
        @NotBlank String targetIssueKey,
        @NotNull IssueRelationType relationType) {}
