package com.tissue.api.invitation.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.workspace.domain.port.out.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvitationValidator {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	public void validateInvitation(Long memberId, String workspaceKey) {
		boolean alreadyJoined = workspaceMemberRepository.existsByMember_IdAndWorkspace_Key(memberId, workspaceKey);

		if (alreadyJoined) {
			// TODO: WorkspaceMemberAlreadyJoinedException, 더 좋은 이름 있나? InvitationFailedException?
			throw new RuntimeException(
				String.format("Member with id %d already joined workspace %s", memberId, workspaceKey)
			);
		}
	}
}
