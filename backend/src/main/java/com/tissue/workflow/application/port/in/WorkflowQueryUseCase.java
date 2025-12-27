package com.tissue.workflow.application.port.in;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.project.application.service.authorization.ProjectAuthExpressions;
import com.tissue.workflow.application.dto.response.WorkflowDetail;
import com.tissue.workflow.application.dto.response.WorkflowSummary;

@Transactional(readOnly = true)
public interface WorkflowQueryUseCase {

	@PreAuthorize(ProjectAuthExpressions.REQUIRES_PROJECT_VIEWER)
	List<WorkflowSummary> getWorkflows(
		String workspaceKey,
		String projectKey,
		boolean includeArchived
	);

	@PreAuthorize(ProjectAuthExpressions.REQUIRES_PROJECT_VIEWER)
	WorkflowDetail getWorkflowDetail(
		String workspaceKey,
		String projectKey,
		Long workflowId
	);
}
