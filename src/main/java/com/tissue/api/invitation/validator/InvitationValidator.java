package com.tissue.api.invitation.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.AlreadyJoinedWorkspaceException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvitationValidator {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	public void validateInvitation(Long memberId, String workspaceCode) {
		if (isAlreadyWorkspaceMember(memberId, workspaceCode)) {
			throw new AlreadyJoinedWorkspaceException();
		}
	}

	private boolean isAlreadyWorkspaceMember(Long memberId, String workspaceCode) {
		return workspaceMemberRepository.existsByMemberIdAndWorkspaceCode(memberId, workspaceCode);
	}

}
