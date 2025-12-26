package com.tissue.security.authorization.project;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.security.authentication.MemberUserDetails;

// TODO: should i add javadoc that explains each permission?
public interface ProjectSecurityExpressions {

	/**
	 * @see ProjectSecurityGuard#isViewer(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_VIEWER = "@projectSecurityGuard.isViewer(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#isMember(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_MEMBER = "@projectSecurityGuard.isMember(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#isAdmin(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_ADMIN = "@projectSecurityGuard.isAdmin(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#canJoinViaDirectAccess(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_JOIN_PERMISSION = "@projectSecurityGuard.canJoinViaDirectAccess(#cmd.workspaceKey, #cmd.projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#canGrantRole(String, String, ProjectRole, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_ROLE_GRANT_PERMISSION = "@projectSecurityGuard.canGrantRole(#cmd.workspaceKey, #cmd.projectKey, #cmd.grantRole, principal)";

	/**
	 * @see ProjectSecurityGuard#canEditSprint(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_SPRINT_EDIT_PERMISSION = "@sprintSecurityGuard.canEditSprint(#cmd.workspaceKey, #cmd.projectKey, #cmd.sprintId, principal)";

	/**
	 * @see ProjectSecurityGuard#canEditIssueType(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_TYPE_EDIT_PERMISSION = "@issueConfigSecurityGuard.canEditIssueType(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueTypeId, principal)";

	/**
	 * @see ProjectSecurityGuard#canEditWorkflow(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_WORKFLOW_EDIT_PERMISSION = "@workflowSecurityGuard.canEditWorkflow(#cmd.workpspaceKey, #cmd.projectKey, #cmd.workflowId, principal)";
}
