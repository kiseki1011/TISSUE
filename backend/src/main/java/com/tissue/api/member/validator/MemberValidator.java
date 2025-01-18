package com.tissue.api.member.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.DuplicateResourceException;
import com.tissue.api.common.exception.InvalidCredentialsException;
import com.tissue.api.common.exception.InvalidOperationException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberValidator {

	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final PasswordEncoder passwordEncoder;

	public void validateMemberPassword(String password, Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));

		validatePasswordMatch(password, member.getPassword());
	}

	public void validatePasswordMatch(String rawPassword, String encodedPassword) {
		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new InvalidCredentialsException("Password is invalid.");
		}
	}

	public void validateLoginIdIsUnique(String loginId) {
		if (memberRepository.existsByLoginId(loginId)) {
			throw new DuplicateResourceException(
				String.format("Login ID already exists. loginId: %s", loginId)
			);
		}
	}

	public void validateEmailIsUnique(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateResourceException(
				String.format("Email already exists. email: %s", email)
			);
		}
	}

	public void validateMemberHasNoOwnedWorkspaces(Long memberId) {
		boolean hasOwnedWorkspaces = workspaceMemberRepository.existsByMemberIdAndRole(memberId, WorkspaceRole.OWNER);
		if (hasOwnedWorkspaces) {
			throw new InvalidOperationException("You currently have one or more owned workspaces.");
		}
	}
}
