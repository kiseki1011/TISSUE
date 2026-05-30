package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowVcsSettingsCommand;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;

public interface WorkflowCommandUseCase {

    WorkflowCreateResponse create(CreateWorkflowCommand cmd, Long actorMemberId);

    void update(Long workflowId, UpdateWorkflowCommand cmd, Long actorMemberId);

    void delete(Long workflowId, Long actorMemberId);

    void updateState(Long workflowId, Long stateId, UpdateStateCommand cmd, Long actorMemberId);

    void updateTransition(Long workflowId, Long transitionId, UpdateTransitionCommand cmd, Long actorMemberId);

    void configureTransitionGuards(
            Long workflowId, Long transitionId, ConfigureTransitionGuardsCommand cmd, Long actorMemberId);

    void updateVcsSettings(Long workflowId, UpdateWorkflowVcsSettingsCommand cmd, Long actorMemberId);
}
