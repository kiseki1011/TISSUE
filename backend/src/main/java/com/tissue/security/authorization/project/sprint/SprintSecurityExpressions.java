package com.tissue.security.authorization.project.sprint;

// TODO: should i integrate into ProjectSecurityExpressions?
public interface SprintSecurityExpressions {

	String REQUIRES_SPRINT_MANAGER = "@sprintSecurityGuard.isSprintManager(#cmd.sprintId, #cmd.projectKey, principal.memberId)";
}
