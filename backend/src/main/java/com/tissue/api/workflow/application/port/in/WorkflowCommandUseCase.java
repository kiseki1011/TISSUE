package com.tissue.api.workflow.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;
import static com.tissue.api.security.authorization.WorkflowSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.api.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.api.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.api.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.api.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.api.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.api.workflow.application.dto.response.WorkflowCreateResponse;

@Transactional
public interface WorkflowCommandUseCase {

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	WorkflowCreateResponse create(CreateWorkflowCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_MANAGER)
	void update(UpdateWorkflowCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_MANAGER)
	void delete(DeleteWorkflowCommand cmd);

	// TODO: restore()

	@PreAuthorize(REQUIRES_WORKFLOW_MANAGER)
	void updateState(UpdateStateCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_MANAGER)
	void updateTransition(UpdateTransitionCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_MANAGER)
	void configureTransitionGuards(ConfigureTransitionGuardsCommand cmd);
}
