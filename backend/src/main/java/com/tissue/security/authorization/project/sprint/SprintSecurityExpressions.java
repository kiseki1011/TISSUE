package com.tissue.security.authorization.project.sprint;

import com.tissue.security.authentication.MemberUserDetails;

// TODO: should i integrate into ProjectSecurityExpressions?
// TODO: should i add javadoc that explains each permission?
public interface SprintSecurityExpressions {

	/**
	 * @see SprintSecurityGuard#canEditSprint(String, String, Long, MemberUserDetails)
	 */
	String REQUIRES_SPRINT_EDIT_PERMISSION = "@sprintSecurityGuard.canEditSprint(#workspaceKey, #projectKey, #sprintId, principal)";
}
