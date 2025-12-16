package com.tissue.workspace.application.service.finder;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.workspace.application.port.out.InvitationQueryRepository;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.exception.InvitationNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvitationFinder {

	private final InvitationQueryRepository invitationQueryRepository;

	public Invitation findBy(Long id) {
		return invitationQueryRepository.findById(id)
			.orElseThrow(() -> new InvitationNotFoundException(id));
	}

	public Set<Long> findPendingMemberIds(String workspaceKey, Collection<Long> memberIds) {
		return invitationQueryRepository.findPendingMemberIds(workspaceKey, memberIds);
	}
}
