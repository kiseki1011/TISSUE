package com.tissue.security.authorization;

// TODO: 각 expression을 설명하는 주석
//  - 사용하는 메서드로의 참조 추가하면 좋을듯?
public interface SprintSecurityExpressions {

	String REQUIRES_SPRINT_MANAGER = "@sprintSecurityGuard.isSprintManager(#cmd.sprintId, principal.memberId)";
}
