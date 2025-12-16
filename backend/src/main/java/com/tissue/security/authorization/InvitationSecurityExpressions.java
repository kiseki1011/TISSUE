package com.tissue.security.authorization;

public interface InvitationSecurityExpressions {

	String REQUIRES_INVITATION_OWNER = "@invitationSecurityGuard.isOwner(#invitationId, principal.memberId)";
}
