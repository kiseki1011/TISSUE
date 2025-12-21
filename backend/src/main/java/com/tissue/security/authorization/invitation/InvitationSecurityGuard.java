package com.tissue.security.authorization.invitation;

import org.springframework.stereotype.Component;

import com.tissue.workspace.application.port.out.InvitationQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvitationSecurityGuard {

	private final InvitationQueryRepository invitationRepository;

	public boolean isOwner(Long invitationId, Long memberId) {
		return invitationRepository.findById(invitationId)
			.map(invitation -> invitation.getMember().getId().equals(memberId))
			.orElse(false);
	}
}
