package com.uranus.taskmanager.api.member.service;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberEmailUpdateRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberPasswordUpdateRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.MemberEmailUpdateResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.SignupResponse;
import com.uranus.taskmanager.api.member.exception.DuplicateEmailException;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class MemberServiceTest extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("회원 가입에 성공하면 멤버가 저장된다")
	void signup_sucess_memberIsSaved() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser")
			.password("testpassword1234!")
			.email("testemail@test.com")
			.build();

		// when
		SignupResponse signupResponse = memberService.signup(signupRequest);

		// then
		assertThat(signupResponse.getLoginId()).isEqualTo("testuser");
		assertThat(signupResponse.getEmail()).isEqualTo("testemail@test.com");
	}

	@Test
	@DisplayName("회원 가입에 성공하여 저장된 멤버의 패스워드는 암호화 되어 있다")
	void signup_sucess_memberPasswordIsEncrypted() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser")
			.password("testpassword1234!")
			.email("testemail@test.com")
			.build();

		// when
		memberService.signup(signupRequest);

		// then
		Optional<Member> member = memberRepository.findByLoginId("testuser");
		String encodedPassword = member.get().getPassword();
		assertThat(passwordEncoder.matches("testpassword1234!", encodedPassword)).isTrue();
	}

	@Test
	@DisplayName("회원 가입 시 입력한 패스워드와 암호화한 패스워드는 서로 다르다")
	void signup_sucess_requestPaswordMustBeDifferentWithEncryptedPassword() {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser")
			.password("testpassword1234!")
			.email("testemail@test.com")
			.build();

		// when
		memberService.signup(signupRequest);

		// then
		Optional<Member> member = memberRepository.findByLoginId("testuser");
		String encodedPassword = member.get().getPassword();
		assertThat(encodedPassword).isNotEqualTo("testpassword1234!");
	}

	@Test
	@DisplayName("이메일 업데이트를 성공하면 이메일 업데이트 응답을 반환한다")
	void updateEmail_success_returnsMemberEmailUpdateResponse() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		String newEmail = "newemail@test.com";
		MemberEmailUpdateRequest request = new MemberEmailUpdateRequest(newEmail);

		// when
		MemberEmailUpdateResponse response = memberService.updateEmail(request, member.getId());

		// then
		assertThat(response.getMemberDetail().getEmail()).isEqualTo(newEmail);

		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getEmail()).isEqualTo(newEmail);
	}

	@Test
	@DisplayName("이메일 업데이트 시 이메일이 중복되면 예외가 발생한다")
	void updateEmail_throwsException_whenEmailDuplicated() {
		// given
		Member existingMember = memberRepository.save(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.password(passwordEncoder.encode("password1"))
				.build()
		);

		MemberEmailUpdateRequest request = new MemberEmailUpdateRequest(existingMember.getEmail());

		// when & then
		assertThatThrownBy(() -> memberService.updateEmail(request, existingMember.getId()))
			.isInstanceOf(DuplicateEmailException.class);
	}

	@Test
	@DisplayName("패스워드 업데이트를 성공하면 아무것도 반환하지 않는다")
	void updatePassword_success_returnsNothing() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		String newPassword = "newpassword1234!";
		MemberPasswordUpdateRequest request = new MemberPasswordUpdateRequest(newPassword);

		// when & then
		assertThatNoException().isThrownBy(() -> memberService.updatePassword(request, member.getId()));
	}

	@Test
	@DisplayName("패스워드 업데이트를 성공하면 업데이트 된 멤버는 암호화된 새로운 패스워드를 가진다")
	void updatePassword_success_updatedMemberHasNewEncrytedPassword() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		String newPassword = "newpassword1234!";
		MemberPasswordUpdateRequest request = new MemberPasswordUpdateRequest(newPassword);

		// when
		memberService.updatePassword(request, member.getId());

		// then
		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(passwordEncoder.matches(newPassword, updatedMember.getPassword())).isTrue();
	}
}
