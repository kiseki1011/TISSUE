package com.tissue.security.authorization.project;

// TODO: add javadoc that explains each permission
public interface ProjectSecurityExpressions {

	String REQUIRES_PROJECT_VIEWER = "@projectSecurityGuard.hasReadPermission(#workspaceKey, #projectKey, principal.memberId)";

	String REQUIRES_PROJECT_MEMBER = "@projectSecurityGuard.isMember(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_ADMIN = "@projectSecurityGuard.isAdmin(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_JOINABLE = "@projectSecurityGuard.canJoin(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_GRANTABLE_PROJECT_ROLE = "@projectSecurityGuard.canGrantRole(#cmd.workspaceKey, #cmd.projectKey, principal.memberId, #cmd.role)";

	String REQUIRES_TARGET_PROJECTS_ADMIN = "@projectSecurityGuard.hasProjectAdminPermission(#cmd.workspaceKey, #cmd.extractProjectKeys(), principal.memberId)";

	String REQUIRES_ISSUE_TYPE_MANAGE = "@issueConfigSecurityGuard.canManageIssueType(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueTypeId, principal.memberId)";
}
