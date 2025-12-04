package com.tissue.api.workspace.application.port.in;

import static com.tissue.api.security.authorization.InvitationSecurityExpressions.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.response.query.InvitationDetail;

public interface InvitationUseCase {

	@Transactional
	@PreAuthorize(REQUIRES_INVITATION_OWNER)
	void accept(Long memberId, Long invitationId);

	@Transactional
	@PreAuthorize(REQUIRES_INVITATION_OWNER)
	void reject(Long memberId, Long invitationId);

	@Transactional(readOnly = true)
	@PreAuthorize("#memberId == principal.memberId")
	List<InvitationDetail> getMyInvitations(Long memberId);
}
