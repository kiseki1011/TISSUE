package com.tissue.issue.adapter.web.request;

import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotNull;

public record PerformTransitionRequest(@NotNull Long transitionId) {

    public PerformTransitionCommand toCommand(String issueKey, ProjectMemberContext actorContext) {
        return new PerformTransitionCommand(issueKey, transitionId, actorContext);
    }
}
