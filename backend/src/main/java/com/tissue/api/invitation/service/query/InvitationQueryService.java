package com.tissue.api.invitation.service.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.invitation.domain.repository.InvitationQueryRepository;
import com.tissue.api.invitation.presentation.dto.InvitationSearchCondition;
import com.tissue.api.invitation.presentation.dto.response.InvitationDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationQueryService {

	private final InvitationQueryRepository invitationQueryRepository;

	@Transactional(readOnly = true)
	public Page<InvitationDetail> getInvitations(
		Long memberId,
		InvitationSearchCondition searchCondition,
		Pageable pageable
	) {
		return invitationQueryRepository.findAllByMemberIdAndStatusIn(
			memberId,
			searchCondition.statuses(),
			pageable
		).map(InvitationDetail::from);
	}
}
