package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.shared.dto.ProjectIdentifier;

public interface WorkflowCommandUseCase {

    WorkflowCreateResponse create(ProjectIdentifier projectIdentifier, CreateWorkflowCommand cmd, Long memberId);

    void update(ProjectIdentifier projectIdentifier, Long workflowId, UpdateWorkflowCommand cmd, Long memberId);

    void delete(ProjectIdentifier projectIdentifier, Long workflowId, Long memberId);

    void updateState(
            ProjectIdentifier projectIdentifier, Long workflowId, Long stateId, UpdateStateCommand cmd, Long memberId);

    void updateTransition(
            ProjectIdentifier projectIdentifier,
            Long workflowId,
            Long transitionId,
            UpdateTransitionCommand cmd,
            Long memberId);

    void configureTransitionGuards(
            ProjectIdentifier projectIdentifier,
            Long workflowId,
            Long transitionId,
            ConfigureTransitionGuardsCommand cmd,
            Long memberId);
}
