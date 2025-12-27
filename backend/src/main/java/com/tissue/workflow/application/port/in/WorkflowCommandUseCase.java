package com.tissue.workflow.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.workflow.application.dto.response.WorkflowCreateResponse;

@Transactional
public interface WorkflowCommandUseCase {

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	WorkflowCreateResponse create(CreateWorkflowCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_EDIT_PERMISSION)
	void update(UpdateWorkflowCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_EDIT_PERMISSION)
	void delete(DeleteWorkflowCommand cmd);

	// TODO: restore()

	@PreAuthorize(REQUIRES_WORKFLOW_EDIT_PERMISSION)
	void updateState(UpdateStateCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_EDIT_PERMISSION)
	void updateTransition(UpdateTransitionCommand cmd);

	@PreAuthorize(REQUIRES_WORKFLOW_EDIT_PERMISSION)
	void configureTransitionGuards(ConfigureTransitionGuardsCommand cmd);
}
