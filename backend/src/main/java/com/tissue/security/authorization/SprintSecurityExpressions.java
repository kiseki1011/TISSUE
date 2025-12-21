package com.tissue.security.authorization;

// TODO: integrate into ProjectSecurityExpressions
public interface SprintSecurityExpressions {

	String REQUIRES_SPRINT_MANAGER = "@sprintSecurityGuard.isSprintManager(#cmd.sprintId, principal.memberId)";
}
