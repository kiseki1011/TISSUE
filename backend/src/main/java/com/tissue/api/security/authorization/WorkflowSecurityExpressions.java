package com.tissue.api.security.authorization;

public interface WorkflowSecurityExpressions {

	String REQUIRES_WORKFLOW_MANAGER = "@workflowSecurityGuard.isWorkflowManager(#cmd.workflowId, principal.memberId)";
}
