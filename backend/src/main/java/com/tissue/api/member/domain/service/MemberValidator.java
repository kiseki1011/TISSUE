package com.tissue.api.member.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.exception.DuplicateEmailException;
import com.tissue.api.member.exception.DuplicateUsernameException;
import com.tissue.api.member.exception.MemberHasOwnedWorkspacesException;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;
import com.tissue.api.workspace.domain.port.out.WorkspaceMemberCommandRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberValidator {

	private final MemberRepository memberRepository;
	private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

	public void ensureEmailIsUnique(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateEmailException(email);
		}
	}

	public void ensureUsernameIsUnique(String username) {
		if (memberRepository.existsByUsername(username)) {
			throw new DuplicateUsernameException(username);
		}
	}

	public void ensureWithdrawable(Member member) {
		String message = "Member(id: %s, username:'%s') cannot withdraw. Delete or transfer ownership of all owned workspaces."
			.formatted(member.getId(), member.getUsername());

		boolean hasOwnedWorkspaces = workspaceMemberCommandRepository.existsByMemberAndRole(member,
			WorkspaceRole.OWNER);
		if (hasOwnedWorkspaces) {
			throw new MemberHasOwnedWorkspacesException(
				message,
				member.getId(),
				member.getUsername()
			);
		}
	}
}
