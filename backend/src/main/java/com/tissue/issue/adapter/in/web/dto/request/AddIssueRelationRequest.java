package com.tissue.issue.adapter.in.web.dto.request;

import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.domain.enums.IssueRelationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddIssueRelationRequest(
        @NotBlank String targetProjectKey,
        @NotBlank String targetIssueKey,
        @NotNull IssueRelationType relationType) {
    public AddIssueRelationCommand toCommand(
            String workspaceKey,
            String sourceProjectKey,
            String sourceIssueKey,
            Long currentMemberId) {
        return AddIssueRelationCommand.builder()
                .workspaceKey(workspaceKey)
                .sourceProjectKey(sourceProjectKey)
                .sourceIssueKey(sourceIssueKey)
                .targetProjectKey(targetProjectKey)
                .targetIssueKey(targetIssueKey)
                .relationType(relationType)
                .actorMemberId(currentMemberId)
                .build();
    }
}
