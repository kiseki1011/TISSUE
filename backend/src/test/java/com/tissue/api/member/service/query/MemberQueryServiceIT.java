package com.tissue.api.member.service.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.presentation.dto.response.MyProfileResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class MemberQueryServiceIT extends ServiceIntegrationTestHelper {

	@BeforeEach
	public void setUp() {
		memberRepositoryFixture.createAndSaveMember("testuser", "test@test.com", "test1234!");
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("나의 상세 정보를 조회할 수 있다")
	void test() {
		// given
		Member member = memberRepository.findByLoginId("testuser").orElseThrow();
		Long memberId = member.getId();

		// when
		MyProfileResponse response = memberQueryService.getMyProfile(memberId);

		// then
		assertThat(response.loginId()).isEqualTo("testuser");
		assertThat(response.email()).isEqualTo("test@test.com");
	}
}