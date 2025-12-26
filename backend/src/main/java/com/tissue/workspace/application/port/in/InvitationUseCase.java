package com.tissue.workspace.application.port.in;

import java.util.List;

import com.tissue.workspace.application.dto.out.query.InvitationDetail;

public interface InvitationUseCase {

	void accept(Long memberId, Long invitationId);

	void reject(Long memberId, Long invitationId);

	List<InvitationDetail> getMyInvitations(Long memberId);
}
