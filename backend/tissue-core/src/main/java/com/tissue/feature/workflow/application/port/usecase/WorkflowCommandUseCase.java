package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowVcsSettingsCommand;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.shared.dto.ProjectIdentifier;

public interface WorkflowCommandUseCase {

    WorkflowCreateResponse create(ProjectIdentifier pid, CreateWorkflowCommand cmd, Long actorMemberId);

    void update(String workspaceKey, Long workflowId, UpdateWorkflowCommand cmd, Long actorMemberId);

    void delete(String workspaceKey, Long workflowId, Long actorMemberId);

    void updateState(String workspaceKey, Long workflowId, Long stateId, UpdateStateCommand cmd, Long actorMemberId);

    void updateTransition(
            String workspaceKey, Long workflowId, Long transitionId, UpdateTransitionCommand cmd, Long actorMemberId);

    void configureTransitionGuards(
            String workspaceKey,
            Long workflowId,
            Long transitionId,
            ConfigureTransitionGuardsCommand cmd,
            Long actorMemberId);

    void updateVcsSettings(
            String workspaceKey, Long workflowId, UpdateWorkflowVcsSettingsCommand cmd, Long actorMemberId);
}
