package com.tissue.security.authorization.project.workflow;

// TODO: integrate into ProjectSecurityExpressions
public interface WorkflowSecurityExpressions {

	String REQUIRES_WORKFLOW_MANAGER = "@workflowSecurityGuard.isWorkflowManager(#cmd.workflowId, principal.memberId)";
}
