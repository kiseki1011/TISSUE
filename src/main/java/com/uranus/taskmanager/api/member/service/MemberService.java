package com.uranus.taskmanager.api.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.DuplicateEmailException;
import com.uranus.taskmanager.api.member.exception.DuplicateLoginIdException;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.exception.OwnedWorkspaceExistsException;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberEmailUpdateRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberPasswordUpdateRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberWithdrawRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.MemberEmailUpdateResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.SignupResponse;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.security.authentication.exception.InvalidLoginPasswordException;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		checkLoginIdAndEmailDuplication(request);
		Member member = createMember(request);

		return SignupResponse.from(memberRepository.save(member));
	}

	@Transactional
	public MemberEmailUpdateResponse updateEmail(MemberEmailUpdateRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		String toBeEmail = request.getUpdateEmail();

		checkEmailDuplicate(toBeEmail);

		member.updateEmail(toBeEmail);

		return MemberEmailUpdateResponse.from(member);
	}

	@Transactional
	public void updatePassword(MemberPasswordUpdateRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		String toBePassword = encodePassword(request.getUpdatePassword());

		member.updatePassword(toBePassword);
	}

	@Transactional
	public void withdrawMember(MemberWithdrawRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		validatePassword(request.getPassword(), member.getPassword());

		boolean workspaceExists = workspaceMemberRepository.existsByMemberIdAndRole(id, WorkspaceRole.OWNER);
		if (workspaceExists) {
			throw new OwnedWorkspaceExistsException();
		}

		memberRepository.delete(member);
	}

	private void validatePassword(String rawPassword, String encodedPassword) {
		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new InvalidLoginPasswordException();
		}
	}

	private Member createMember(SignupRequest request) {
		String encodedPassword = encodePassword(request.getPassword());

		// Todo: SignupRequest에 to()를 만들어서 사용하기
		return Member.builder()
			.loginId(request.getLoginId())
			.email(request.getEmail())
			.password(encodedPassword)
			.build();
	}

	private void checkLoginIdAndEmailDuplication(SignupRequest request) {
		checkLoginIdDuplicate(request.getLoginId());
		checkEmailDuplicate(request.getEmail());
	}

	private void checkLoginIdDuplicate(String loginId) {
		if (memberRepository.existsByLoginId(loginId)) {
			throw new DuplicateLoginIdException();
		}
	}

	private void checkEmailDuplicate(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}
	}

	private String encodePassword(String password) {
		return passwordEncoder.encode(password);
	}
}
