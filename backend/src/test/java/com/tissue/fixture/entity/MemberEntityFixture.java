package com.tissue.fixture.entity;

import com.tissue.api.member.domain.Member;

public class MemberEntityFixture {
	public Member createMember(String loginId, String email) {
		return Member.builder()
			.loginId(loginId)
			.email(email)
			.password("test1234!")
			.build();
	}
}
