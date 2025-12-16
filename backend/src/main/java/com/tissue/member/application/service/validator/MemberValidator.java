package com.tissue.member.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.DuplicateEmailException;
import com.tissue.member.domain.exception.DuplicateUsernameException;
import com.tissue.member.domain.exception.MemberHasOwnedWorkspacesException;
import com.tissue.member.application.port.out.MemberRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberValidator {

	private final MemberRepository memberRepository;
	private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

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

		boolean hasOwnedWorkspaces = workspaceMemberQueryRepository.existsByMemberAndRole(member,
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
