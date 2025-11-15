package com.tissue.api.invitation.application.service.finder;

import org.springframework.stereotype.Service;

import com.tissue.api.invitation.domain.enums.InvitationStatus;
import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationFinder {

	private final InvitationRepository invitationRepository;

	public Invitation findInvitation(Long invitationId) {
		return invitationRepository.findById(invitationId)
			// TODO: InvitationNotFoundException
			.orElseThrow(() -> new RuntimeException(
				String.format("Invitation not found with invitation id: %d", invitationId)));
	}

	public Invitation findPendingInvitation(Long invitationId) {
		return invitationRepository.findByIdAndStatus(invitationId, InvitationStatus.PENDING)
			// TODO: InvitationNotFoundException
			.orElseThrow(() -> new RuntimeException(
				String.format("Pending invitation not found with id: %d", invitationId)));
	}
}
