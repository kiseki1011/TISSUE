package com.tissue.api.security.authorization;

public interface SprintSecurityExpressions {

	String REQUIRES_SPRINT_MANAGER = "@sprintSecurityGuard.isSprintManager(#cmd.sprintId, principal.memberId)";
}
