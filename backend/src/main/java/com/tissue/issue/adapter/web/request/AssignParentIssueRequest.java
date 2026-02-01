package com.tissue.issue.adapter.web.request;

import com.tissue.issue.application.dto.request.AssignParentCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record AssignParentIssueRequest(
        @NotBlank String parentProjectKey, @NotBlank String parentIssueKey) {

    public AssignParentCommand toCommand(String issueKey, ProjectMemberContext actorContext) {
        return AssignParentCommand.builder()
                .issueKey(issueKey)
                .parentIssueKey(parentIssueKey)
                .parentProjectKey(parentProjectKey)
                .actorContext(actorContext)
                .build();
    }
}
