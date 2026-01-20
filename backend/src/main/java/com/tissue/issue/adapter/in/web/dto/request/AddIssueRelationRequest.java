package com.tissue.issue.adapter.in.web.dto.request;

import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddIssueRelationRequest(
        @NotBlank String targetProjectKey,
        @NotBlank String targetIssueKey,
        @NotNull IssueRelationType relationType) {

    public AddIssueRelationCommand toCommand(String sourceIssueKey, ProjectMemberContext actorContext) {
        return AddIssueRelationCommand.builder()
                .sourceIssueKey(sourceIssueKey)
                .targetProjectKey(targetProjectKey)
                .targetIssueKey(targetIssueKey)
                .relationType(relationType)
                .actorContext(actorContext)
                .build();
    }
}
