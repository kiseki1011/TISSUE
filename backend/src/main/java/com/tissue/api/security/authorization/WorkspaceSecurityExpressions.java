package com.tissue.api.security.authorization;

public interface WorkspaceSecurityExpressions {

	String REQUIRES_SELF_MODIFICATION = "@workspaceSecurityGuard.isSelfModification(#cmd.actorMemberId(), principal.memberId)";

	String REQUIRES_MEMBER = "@workspaceSecurityGuard.isMember(#cmd.workspaceKey(), principal.memberId)";

	String REQUIRES_ADMIN = "@workspaceSecurityGuard.isAdmin(#cmd.workspaceKey(), principal.memberId)";

	String REQUIRES_OWNER = "@workspaceSecurityGuard.isOwner(#cmd.workspaceKey(), principal.memberId)";

	String REQUIRES_HIGHER_ROLE_THAN_TARGET = "@workspaceSecurityGuard.targetHasLowerRole(#cmd.workspaceKey(), #cmd.targetMemberId(), principal.memberId)";

	String AND = " AND ";

	String OR = " OR ";
}
