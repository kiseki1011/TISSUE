package com.tissue.workspace.application.service.authorization;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public interface WorkspaceAuthExpressions {

    /**
     * @see WorkspaceAuthorizationService#isMember(String, MemberUserDetails)
     */
    String REQUIRES_WORKSPACE_MEMBER = "@workspaceSecurityGuard.isMember(#cmd.workspaceKey, principal)";

    /**
     * @see WorkspaceAuthorizationService#isAdmin(String, MemberUserDetails)
     */
    String REQUIRES_WORKSPACE_ADMIN = "@workspaceSecurityGuard.isAdmin(#cmd.workspaceKey, principal)";

    /**
     * @see WorkspaceAuthorizationService#isOwner(String, MemberUserDetails)
     */
    String REQUIRES_WORKSPACE_OWNER = "@workspaceSecurityGuard.isOwner(#cmd.workspaceKey, principal)";

    /**
     * @see WorkspaceAuthorizationService#isSelfModification(String, Long, MemberUserDetails)
     */
    String REQUIRES_SELF =
            "@workspaceSecurityGuard.isSelfModification(#cmd.workspaceKey, #cmd.memberId," + " principal)";

    /**
     * @see WorkspaceAuthorizationService#hasHigherRoleThanTarget(String, Long, MemberUserDetails)
     */
    String REQUIRES_HIGHER_WORKSPACE_ROLE =
            "@workspaceSecurityGuard.hasHigherRoleThanTarget(#cmd.workspaceKey," + " #cmd.targetMemberId, principal)";

    /**
     * @see WorkspaceAuthorizationService#canGrantRole(String, WorkspaceRole, MemberUserDetails)
     */
    String REQUIRES_WORKSPACE_ROLE_GRANT_PERMISSION =
            "@workspaceSecurityGuard.canGrantRole(#cmd.workspaceKey, #cmd.grantRole, principal)";

    /**
     * @see WorkspaceAuthorizationService#canEditInviteLink(String, String, MemberUserDetails)
     */
    String REQUIRES_LINK_EDIT_PERMISSION =
            "@workspaceSecurityGuard.canEditLink(#cmd.workspaceKey, #cmd.token, principal)";
}
