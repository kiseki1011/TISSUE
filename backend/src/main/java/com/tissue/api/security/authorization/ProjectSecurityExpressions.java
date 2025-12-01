package com.tissue.api.security.authorization;

// TODO: 각 expression을 설명하는 주석
//  - 사용하는 메서드로의 참조 추가하면 좋을듯?
public interface ProjectSecurityExpressions {

	String REQUIRES_PROJECT_ACCESS = "@projectSecurityGuard.hasReadPermission(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_WRITER = "@projectSecurityGuard.hasWritePermission(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_ADMIN = "@projectSecurityGuard.isAdmin(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_PROJECT_JOINABLE = "@projectSecurityGuard.canJoin(#cmd.workspaceKey, #cmd.projectKey, principal.memberId)";

	String REQUIRES_TARGET_PROJECTS_ADMIN = "@projectSecurityGuard.hasProjectAdminPermission(#cmd.workspaceKey, #cmd.extractProjectKeys(), principal.memberId)";
}
