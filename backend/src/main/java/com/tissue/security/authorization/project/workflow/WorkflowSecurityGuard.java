package com.tissue.security.authorization.project.workflow;

import org.springframework.stereotype.Component;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowSecurityGuard {

	private final WorkflowQueryRepository workflowQueryRepository;
	// private final ProjectSecurityGuard projectSecurityGuard;

	public boolean canEditWorkflow(String workspaceKey, String projectKey, Long workflowId,
		MemberUserDetails userDetails) {
		// TODO: should i use projectSecurityGuard.isAdmin?
		return userDetails.hasProjectRole(workspaceKey, projectKey, ProjectRole.ADMIN)
			|| isWorkflowCreator(workflowId, userDetails);
	}

	// TODO: Boolean vs boolean?
	private boolean isWorkflowCreator(Long workflowId, MemberUserDetails userDetails) {
		return workflowQueryRepository.findById(workflowId)
			.map(workflow -> workflow.getCreatedBy().equals(userDetails.getMemberId()))
			.orElse(false);
	}
}
