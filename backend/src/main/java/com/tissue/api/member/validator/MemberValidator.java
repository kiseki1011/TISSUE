package com.tissue.api.member.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.DuplicateEmailException;
import com.tissue.api.member.exception.DuplicateLoginIdException;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.member.exception.OwnedWorkspaceExistsException;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.security.authentication.exception.InvalidLoginPasswordException;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberValidator {

	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final PasswordEncoder passwordEncoder;

	public void validateSignup(SignupMemberRequest request) {
		validateLoginIdIsUnique(request.getLoginId());
		validateEmailIsUnique(request.getEmail());
	}

	public void validateUpdateEmail(String email) {
		validateEmailIsUnique(email);
	}

	public void validateMemberPasswordForPermission(String password, Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		validatePasswordMatch(password, member.getPassword());
	}

	public void validateWithdraw(Long memberId) {
		validateMemberHasNoOwnedWorkspaces(memberId);
	}

	public void validatePasswordMatch(String rawPassword, String encodedPassword) {
		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new InvalidLoginPasswordException();
		}
	}

	private void validateLoginIdIsUnique(String loginId) {
		if (memberRepository.existsByLoginId(loginId)) {
			throw new DuplicateLoginIdException();
		}
	}

	private void validateEmailIsUnique(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();

		}
	}

	private void validateMemberHasNoOwnedWorkspaces(Long memberId) {
		boolean hasOwnedWorkspaces = workspaceMemberRepository.existsByMemberIdAndRole(memberId, WorkspaceRole.OWNER);
		if (hasOwnedWorkspaces) {
			throw new OwnedWorkspaceExistsException();
		}
	}
}
