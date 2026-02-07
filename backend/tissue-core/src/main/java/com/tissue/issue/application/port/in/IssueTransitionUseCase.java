package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.project.application.dto.ProjectMemberContext;

public interface IssueTransitionUseCase {

    void performTransition(String issueKey, Long transitionId, ProjectMemberContext actorContext);

    void performTransitionBySystem(
            String issueKey,
            Long transitionId,
            String workspaceKey,
            String projectKey,
            PerformSystemTransitionCommand cmd);
}
