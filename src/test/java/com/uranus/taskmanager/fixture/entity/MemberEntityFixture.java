package com.uranus.taskmanager.fixture.entity;

import com.uranus.taskmanager.api.member.domain.Member;

public class MemberEntityFixture {
	public Member createMember(String loginId, String email) {
		return Member.builder()
			.loginId(loginId)
			.email(email)
			.password("test1234!")
			.build();
	}
}
