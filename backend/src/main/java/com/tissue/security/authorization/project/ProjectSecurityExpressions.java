package com.tissue.security.authorization.project;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authorization.project.issueconfig.IssueConfigSecurityGuard;

// TODO: should i add javadoc that explains each permission?
public interface ProjectSecurityExpressions {

	/**
	 * @see ProjectSecurityGuard#isViewer(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_VIEWER = "@projectSecurityGuard.isViewer(#workspaceKey, #projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#isMember(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_MEMBER = "@projectSecurityGuard.isMember(#workspaceKey, #projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#isAdmin(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_ADMIN = "@projectSecurityGuard.isAdmin(#workspaceKey, #projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#canJoinViaDirectAccess(String, String, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_JOIN_PERMISSION = "@projectSecurityGuard.canJoinViaDirectAccess(#workspaceKey, #projectKey, principal)";

	/**
	 * @see ProjectSecurityGuard#canGrantRole(String, String, ProjectRole, MemberUserDetails)
	 */
	String REQUIRES_PROJECT_ROLE_GRANT_PERMISSION = "@projectSecurityGuard.canGrantRole(#workspaceKey, #projectKey, #grantRole, principal)";

	/**
	 * @see IssueConfigSecurityGuard#canEditIssueType(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_TYPE_EDIT_PERMISSION = "@issueConfigSecurityGuard.canEditIssueType(#workspaceKey, #projectKey, #issueTypeId, principal)";
}
