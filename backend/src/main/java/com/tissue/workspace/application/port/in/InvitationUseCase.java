package com.tissue.workspace.application.port.in;

import static com.tissue.security.authorization.invitation.InvitationSecurityExpressions.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.workspace.application.dto.response.query.InvitationDetail;

public interface InvitationUseCase {

	@PreAuthorize(REQUIRES_INVITATION_OWNER)
	void accept(Long memberId, Long invitationId);

	@PreAuthorize(REQUIRES_INVITATION_OWNER)
	void reject(Long memberId, Long invitationId);

	@PreAuthorize("#memberId == principal.memberId")
	List<InvitationDetail> getMyInvitations(Long memberId);
}
