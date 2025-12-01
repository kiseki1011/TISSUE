package com.tissue.api.security.authorization;

// TODO: 각 expression을 설명하는 주석
//  - 사용하는 메서드로의 참조 추가하면 좋을듯?
public interface WorkspaceSecurityExpressions {

	String REQUIRES_SELF_MODIFICATION = "@workspaceSecurityGuard.isSelfModification(#cmd.actorMemberId(), principal.memberId)";

	String REQUIRES_WORKSPACE_MEMBER = "@workspaceSecurityGuard.isMember(#cmd.workspaceKey(), principal.memberId)";

	String REQUIRES_WORKSPACE_ADMIN = "@workspaceSecurityGuard.isAdmin(#cmd.workspaceKey(), principal.memberId)";

	String REQUIRES_WORKSPACE_OWNER = "@workspaceSecurityGuard.isOwner(#cmd.workspaceKey(), principal.memberId)";

	String REQUIRES_HIGHER_WORKSPACE_ROLE = "@workspaceSecurityGuard.targetHasLowerRole(#cmd.workspaceKey(), #cmd.targetMemberId(), principal.memberId)";
}
