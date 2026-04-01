package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.shared.dto.IssueIdentifier;

public interface IssueTransitionUseCase {

    void performTransition(IssueIdentifier iid, Long transitionId, Long actorMemberId);

    void performTransitionBySystem(
            String issueKey, Long transitionId, String workspaceKey, PerformSystemTransitionCommand cmd);
}
