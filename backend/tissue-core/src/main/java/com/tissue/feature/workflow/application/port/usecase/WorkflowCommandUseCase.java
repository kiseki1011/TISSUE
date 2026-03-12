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

    void update(ProjectIdentifier pid, Long workflowId, UpdateWorkflowCommand cmd, Long actorMemberId);

    void delete(ProjectIdentifier pid, Long workflowId, Long actorMemberId);

    void updateState(ProjectIdentifier pid, Long workflowId, Long stateId, UpdateStateCommand cmd, Long actorMemberId);

    void updateTransition(
            ProjectIdentifier pid, Long workflowId, Long transitionId, UpdateTransitionCommand cmd, Long actorMemberId);

    void configureTransitionGuards(
            ProjectIdentifier pid,
            Long workflowId,
            Long transitionId,
            ConfigureTransitionGuardsCommand cmd,
            Long actorMemberId);

    void updateVcsSettings(
            ProjectIdentifier pid, Long workflowId, UpdateWorkflowVcsSettingsCommand cmd, Long actorMemberId);
}
