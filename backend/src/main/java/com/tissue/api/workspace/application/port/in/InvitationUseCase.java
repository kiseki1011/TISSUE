package com.tissue.api.workspace.application.port.in;

import static com.tissue.api.security.authorization.InvitationSecurityExpressions.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.response.InvitationResult;
import com.tissue.api.workspace.application.dto.response.query.InvitationDetail;

public interface InvitationUseCase {

	// TODO: @PreAuthorize를 사용하는 것 보다 그냥 서비스 내부에서 비교하는게 성능상 이득이긴 함
	@Transactional
	@PreAuthorize(REQUIRES_INVITATION_OWNER)
	InvitationResult accept(Long memberId, Long invitationId);

	@Transactional
	@PreAuthorize(REQUIRES_INVITATION_OWNER)
	InvitationResult reject(Long memberId, Long invitationId);

	@Transactional(readOnly = true)
	@PreAuthorize("#memberId == principal.memberId")
	List<InvitationDetail> getMyInvitations(Long memberId);
}
