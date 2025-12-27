package com.tissue.project.application.service.authorization;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.security.authentication.MemberUserDetails;

// TODO: should i add javadoc that explains each permission?
public interface ProjectAuthExpressions {

	/**
	 * @see ProjectAuthorizationService#isViewer(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_VIEWER = "@projectSecurityGuard.isViewer(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectAuthorizationService#isMember(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_MEMBER = "@projectSecurityGuard.isMember(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectAuthorizationService#isAdmin(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_ADMIN = "@projectSecurityGuard.isAdmin(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectAuthorizationService#canJoinViaDirectAccess(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_JOIN_PERMISSION = "@projectSecurityGuard.canJoinViaDirectAccess(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectAuthorizationService#canGrantRole(String, String, ProjectRole, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_ROLE_GRANT_PERMISSION = "@projectSecurityGuard.canGrantRole(#cmd.workspaceKey, #cmd.projectKey, #cmd.grantRole, principal)";

	/**
	 * @see ProjectAuthorizationService#canEditSprint(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_SPRINT_EDIT_PERMISSION = "@sprintSecurityGuard.canEditSprint(#cmd.workspaceKey, #cmd.projectKey, #cmd.sprintId, principal)";

	/**
	 * @see ProjectAuthorizationService#canEditIssueType(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_TYPE_EDIT_PERMISSION = "@issueConfigSecurityGuard.canEditIssueType(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueTypeId, principal)";

	/**
	 * @see ProjectAuthorizationService#canEditWorkflow(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_WORKFLOW_EDIT_PERMISSION = "@workflowSecurityGuard.canEditWorkflow(#cmd.workpspaceKey, #cmd.projectKey, #cmd.workflowId, principal)";
}
