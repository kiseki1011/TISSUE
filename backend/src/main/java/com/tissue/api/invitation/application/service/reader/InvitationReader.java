package com.tissue.api.invitation.application.service.reader;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.enums.InvitationStatus;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationReader {

	private final InvitationRepository invitationRepository;

	public Invitation findInvitation(Long invitationId) {
		return invitationRepository.findById(invitationId)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Invitation not found with invitation id: %d", invitationId)));
	}

	public Invitation findPendingInvitation(Long invitationId) {
		return invitationRepository.findByIdAndStatus(invitationId, InvitationStatus.PENDING)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Pending invitation not found with id: %d", invitationId)));
	}
}
