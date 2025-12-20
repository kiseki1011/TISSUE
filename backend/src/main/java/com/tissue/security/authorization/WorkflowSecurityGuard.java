package com.tissue.security.authorization;

import org.springframework.stereotype.Component;

import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.exception.WorkflowExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowSecurityGuard {

	private final WorkflowQueryRepository workflowQueryRepository;
	private final ProjectSecurityGuard projectSecurityGuard;

	public boolean isWorkflowManager(Long workflowId, String projectKey, Long memberId) {
		Workflow workflow = workflowQueryRepository.findById(workflowId)
			.orElseThrow(() -> WorkflowExceptions.notFound(workflowId));

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
