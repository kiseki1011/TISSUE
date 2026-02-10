package com.tissue.feature.workflow.application.port.in;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;

public interface WorkflowCommandUseCase {

    WorkflowCreateResponse create(CreateWorkflowCommand cmd, ProjectMemberContext actorContext);

    void update(Long workflowId, UpdateWorkflowCommand cmd, ProjectMemberContext actorContext);

    void delete(Long workflowId, ProjectMemberContext actorContext);

    void updateState(Long workflowId, Long stateId, UpdateStateCommand cmd, ProjectMemberContext actorContext);

    void updateTransition(
            Long workflowId, Long transitionId, UpdateTransitionCommand cmd, ProjectMemberContext actorContext);

    void configureTransitionGuards(
            Long workflowId,
            Long transitionId,
            ConfigureTransitionGuardsCommand cmd,
            ProjectMemberContext actorContext);
}
