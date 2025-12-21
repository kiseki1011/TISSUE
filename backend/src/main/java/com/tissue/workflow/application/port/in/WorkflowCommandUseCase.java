package com.tissue.workflow.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.security.authorization.project.ProjectSecurityExpressions;
import com.tissue.security.authorization.project.workflow.WorkflowSecurityExpressions;

@Transactional
public interface WorkflowCommandUseCase {

	@PreAuthorize(ProjectSecurityExpressions.REQUIRES_PROJECT_MEMBER)
	WorkflowCreateResponse create(CreateWorkflowCommand cmd);

	@PreAuthorize(WorkflowSecurityExpressions.REQUIRES_WORKFLOW_MANAGER)
	void update(UpdateWorkflowCommand cmd);

	@PreAuthorize(WorkflowSecurityExpressions.REQUIRES_WORKFLOW_MANAGER)
	void delete(DeleteWorkflowCommand cmd);

	// TODO: restore()

	@PreAuthorize(WorkflowSecurityExpressions.REQUIRES_WORKFLOW_MANAGER)
	void updateState(UpdateStateCommand cmd);

	@PreAuthorize(WorkflowSecurityExpressions.REQUIRES_WORKFLOW_MANAGER)
	void updateTransition(UpdateTransitionCommand cmd);

	@PreAuthorize(WorkflowSecurityExpressions.REQUIRES_WORKFLOW_MANAGER)
	void configureTransitionGuards(ConfigureTransitionGuardsCommand cmd);
}
