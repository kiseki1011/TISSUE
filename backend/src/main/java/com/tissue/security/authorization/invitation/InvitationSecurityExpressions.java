package com.tissue.security.authorization.invitation;

public interface InvitationSecurityExpressions {

	String REQUIRES_INVITATION_OWNER = "@invitationSecurityGuard.isOwner(#invitationId, principal.memberId)";
}
