package com.tissue.api.member.service.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.presentation.dto.request.UpdatePermissionRequest;
import com.tissue.api.security.authentication.exception.InvalidLoginPasswordException;
import com.tissue.helper.ServiceIntegrationTestHelper;
import com.tissue.api.member.exception.MemberNotFoundException;

class MemberQueryServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("업데이트 인가의 비밀번호 검증에 성공하면 예외가 발생하지 않는다")
	void validatePasswordForUpdate_ifSuccess_NoException() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		UpdatePermissionRequest request = new UpdatePermissionRequest("password1234!");

		// when & then
		assertThatNoException()
			.isThrownBy(() -> memberQueryService.validatePasswordForUpdate(request, member.getId()));

	}

	@Test
	@DisplayName("업데이트 인가의 비밀번호 검증에 실패하면 예외가 발생한다")
	void validatePasswordForUpdate_ifFail_InvalidMemberPasswordException() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		UpdatePermissionRequest request = new UpdatePermissionRequest("invalidPassword");

		// when & then
		assertThatThrownBy(() -> memberQueryService.validatePasswordForUpdate(request, member.getId()))
			.isInstanceOf(InvalidLoginPasswordException.class);

	}

	@Test
	@DisplayName("업데이트 인가에서 존재하지 않는 회원으로 시도하면 예외가 발생한다")
	void validatePasswordForUpdate_ifFail_MemberNotFoundException() {
		// given
		UpdatePermissionRequest request = new UpdatePermissionRequest("invalidPassword");

		// when & then
		Long invalidMemberId = 999L;

		assertThatThrownBy(() -> memberQueryService.validatePasswordForUpdate(request, invalidMemberId))
			.isInstanceOf(MemberNotFoundException.class);

	}
}