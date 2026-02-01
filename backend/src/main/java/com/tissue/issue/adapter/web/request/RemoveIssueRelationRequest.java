package com.tissue.issue.adapter.web.request;

import com.tissue.issue.application.dto.request.RemoveIssueRelationCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record RemoveIssueRelationRequest(
        @NotBlank String targetProjectKey, @NotBlank String targetIssueKey) {

    public RemoveIssueRelationCommand toCommand(String sourceIssueKey, ProjectMemberContext actorContext) {
        return new RemoveIssueRelationCommand(sourceIssueKey, targetProjectKey, targetIssueKey, actorContext);
    }
}
