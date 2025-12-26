package com.tissue.security.authorization.workspace;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.workspace.domain.enums.WorkspaceRole;

// TODO: should i add javadoc that explains each permission?
public interface WorkspaceSecurityExpressions {

	/**
	 * @see WorkspaceSecurityGuard#isMember(String, MemberUserDetails)
	 */
	String REQUIRES_WORKSPACE_MEMBER = "@workspaceSecurityGuard.isMember(#workspaceKey, principal)";

	/**
	 * @see WorkspaceSecurityGuard#isAdmin(String, MemberUserDetails)
	 */
	String REQUIRES_WORKSPACE_ADMIN = "@workspaceSecurityGuard.isAdmin(#workspaceKey, principal)";

	/**
	 * @see WorkspaceSecurityGuard#isOwner(String, MemberUserDetails)
	 */
	String REQUIRES_WORKSPACE_OWNER = "@workspaceSecurityGuard.isOwner(#workspaceKey, principal)";

	/**
	 * @see WorkspaceSecurityGuard#isSelfModification(String, Long, MemberUserDetails)
	 */
	String REQUIRES_SELF_MODIFICATION = "@workspaceSecurityGuard.isSelfModification(#workspaceKey, #memberId, principal)";

	/**
	 * @see WorkspaceSecurityGuard#hasHigherRoleThanTarget(String, Long, MemberUserDetails)
	 */
	String REQUIRES_HIGHER_WORKSPACE_ROLE = "@workspaceSecurityGuard.hasHigherRoleThanTarget(#workspaceKey, #targetMemberId, principal)";

	/**
	 * @see WorkspaceSecurityGuard#canGrantRole(String, WorkspaceRole, MemberUserDetails)
	 */
	String REQUIRES_WORKSPACE_ROLE_GRANT_PERMISSION = "@workspaceSecurityGuard.canGrantRole(#workspaceKey, #grantRole, principal)";

	/**
	 * @see WorkspaceSecurityGuard#canEditInviteLink(String, String, MemberUserDetails)
	 */
	String REQUIRES_LINK_EDIT_PERMISSION = "@workspaceSecurityGuard.canEditLink(#workspaceKey, #token, principal)";
}
