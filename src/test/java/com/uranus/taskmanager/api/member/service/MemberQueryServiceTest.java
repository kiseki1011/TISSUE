package com.uranus.taskmanager.api.member.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateAuthRequest;
import com.uranus.taskmanager.api.member.exception.InvalidMemberPasswordException;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class MemberQueryServiceTest extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("업데이트 인가의 비밀번호 검증에 성공하면 예외가 발생하지 않는다")
	void validatePasswordForUpdate_ifSuccess_NoException() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		UpdateAuthRequest request = new UpdateAuthRequest("password1234!");

		// when & then
		assertThatNoException()
			.isThrownBy(() -> memberQueryService.validatePasswordForUpdate(request, member.getId()));

	}

	@Test
	@DisplayName("업데이트 인가의 비밀번호 검증에 실패하면 예외가 발생한다")
	void validatePasswordForUpdate_ifFail_InvalidMemberPasswordException() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		UpdateAuthRequest request = new UpdateAuthRequest("invalidPassword");

		// when & then
		assertThatThrownBy(() -> memberQueryService.validatePasswordForUpdate(request, member.getId()))
			.isInstanceOf(InvalidMemberPasswordException.class);

	}

	@Test
	@DisplayName("업데이트 인가에서 존재하지 않는 회원으로 시도하면 예외가 발생한다")
	void validatePasswordForUpdate_ifFail_MemberNotFoundException() {
		// given
		UpdateAuthRequest request = new UpdateAuthRequest("invalidPassword");

		// when & then
		Long invalidMemberId = 999L;

		assertThatThrownBy(() -> memberQueryService.validatePasswordForUpdate(request, invalidMemberId))
			.isInstanceOf(MemberNotFoundException.class);

	}
}