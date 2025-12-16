package com.tissue.security.authorization;

import org.springframework.stereotype.Component;

import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.exception.WorkflowNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowSecurityGuard {

	private final WorkflowQueryRepository workflowQueryRepository;
	private final ProjectSecurityGuard projectSecurityGuard;

	public boolean isWorkflowManager(Long workflowId, Long memberId) {
		// Workflow workflow = workflowQueryRepository.findByIdAndProject_KeyAndProject_Workspace_Key(workflowId, projectKey, workspaceKey)
		// 	.orElseThrow(() -> new WorkflowNotFoundException(workflowId, projectKey, workspaceKey));

		Workflow workflow = workflowQueryRepository.findById(workflowId)
			.orElseThrow(() -> new WorkflowNotFoundException(workflowId));

		if (workflow.getCreatedBy().equals(memberId)) {
			return true;
		}

		return projectSecurityGuard.isAdmin(
			workflow.getWorkspaceKey(),
			workflow.getProjectKey(),
			memberId
		);
	}
}
