package com.tissue.api.security.authorization;

public interface InvitationSecurityExpressions {

	String REQUIRES_INVITATION_OWNER = "@invitationSecurityGuard.isOwner(#invitationId, principal.memberId)";
}
