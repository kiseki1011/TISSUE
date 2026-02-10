package com.tissue.feature.issue.application.port.in;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.project.application.dto.ProjectMemberContext;

public interface IssueTransitionUseCase {

    void performTransition(String issueKey, Long transitionId, ProjectMemberContext actorContext);

    void performTransitionBySystem(
            String issueKey,
            Long transitionId,
            String workspaceKey,
            String projectKey,
            PerformSystemTransitionCommand cmd);
}
