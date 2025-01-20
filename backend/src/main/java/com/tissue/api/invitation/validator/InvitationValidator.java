package com.tissue.api.invitation.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvitationValidator {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	public void validateInvitation(Long memberId, String workspaceCode) {
		if (hasWorkspaceMember(memberId, workspaceCode)) {
			throw new InvalidOperationException(
				String.format("Member with id %d already joined workspace %s", memberId, workspaceCode)
			);
		}
	}

	private boolean hasWorkspaceMember(Long memberId, String workspaceCode) {
		return workspaceMemberRepository.existsByMemberIdAndWorkspaceCode(memberId, workspaceCode);
	}

}
