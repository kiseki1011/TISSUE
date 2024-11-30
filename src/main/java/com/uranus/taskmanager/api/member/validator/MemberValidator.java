package com.uranus.taskmanager.api.member.validator;

import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.DuplicateEmailException;
import com.uranus.taskmanager.api.member.exception.DuplicateLoginIdException;
import com.uranus.taskmanager.api.member.exception.OwnedWorkspaceExistsException;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupMemberRequest;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.security.authentication.exception.InvalidLoginPasswordException;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberValidator {
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	private final PasswordEncoder passwordEncoder;

	public void validateSignup(SignupMemberRequest request) {
		validateLoginIdNotExists(request.getLoginId());
		validateEmailNotExists(request.getEmail());
	}

	public void validateEmailUpdate(String email) {
		validateEmailNotExists(email);
	}

	public void validateWithdraw(Long memberId) {
		validateNoOwnedWorkspaces(memberId);
	}

	public void validatePassword(String rawPassword, String encodedPassword) {
		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new InvalidLoginPasswordException();
		}
	}

	private void validateLoginIdNotExists(String loginId) {
		if (memberRepository.existsByLoginId(loginId)) {
			throw new DuplicateLoginIdException();
		}
	}

	private void validateEmailNotExists(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();

		}
	}

	private void validateNoOwnedWorkspaces(Long memberId) {
		boolean hasOwnedWorkspaces = workspaceMemberRepository.existsByMemberIdAndRole(memberId, WorkspaceRole.OWNER);
		if (hasOwnedWorkspaces) {
			throw new OwnedWorkspaceExistsException();
		}
	}
}
