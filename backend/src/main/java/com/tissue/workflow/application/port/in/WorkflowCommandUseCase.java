package com.tissue.workflow.application.port.in;

import com.tissue.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.workflow.application.dto.response.WorkflowCreateResponse;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface WorkflowCommandUseCase {

    WorkflowCreateResponse create(CreateWorkflowCommand cmd);

    void update(UpdateWorkflowCommand cmd);

    void delete(DeleteWorkflowCommand cmd);

    // TODO: restore()

    void updateState(UpdateStateCommand cmd);

    void updateTransition(UpdateTransitionCommand cmd);

    void configureTransitionGuards(ConfigureTransitionGuardsCommand cmd);
}
