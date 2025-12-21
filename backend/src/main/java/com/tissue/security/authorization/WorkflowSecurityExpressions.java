package com.tissue.security.authorization;

// TODO: integrate into ProjectSecurityExpressions
public interface WorkflowSecurityExpressions {

	String REQUIRES_WORKFLOW_MANAGER = "@workflowSecurityGuard.isWorkflowManager(#cmd.workflowId, principal.memberId)";
}
