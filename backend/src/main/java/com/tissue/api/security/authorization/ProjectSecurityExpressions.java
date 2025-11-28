package com.tissue.api.security.authorization;

public interface ProjectSecurityExpressions {

	String REQUIRES_PROJECT_ACCESS = "@projectSecurityGuard.hasReadPermission(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_WRITER = "@projectSecurityGuard.hasWritePermission(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_ADMIN = "@projectSecurityGuard.isAdmin(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_JOINABLE = "@projectSecurityGuard.canJoin(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";
}
