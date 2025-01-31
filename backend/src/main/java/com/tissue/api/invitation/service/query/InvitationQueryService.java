package com.tissue.api.invitation.service.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.presentation.dto.InvitationSearchCondition;
import com.tissue.api.invitation.presentation.dto.response.InvitationResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationQueryService {

	private final InvitationRepository invitationRepository;

	@Transactional(readOnly = true)
	public Page<InvitationResponse> getInvitations(
		Long memberId,
		InvitationSearchCondition searchCondition,
		Pageable pageable
	) {
		return invitationRepository.findAllByMemberIdAndStatusIn(
			memberId,
			searchCondition.statuses(),
			pageable
		).map(InvitationResponse::from);
	}

	@Transactional(readOnly = true)
	public Invitation findInvitation(Long invitationId) {
		return invitationRepository.findById(invitationId)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Invitation not found with invitation id: %d", invitationId)));
	}
}
