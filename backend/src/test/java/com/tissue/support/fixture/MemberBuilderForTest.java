package com.tissue.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.tissue.api.member.domain.model.Member;

public class MemberBuilderForTest {

	private Long id = 1L;
	private String loginId = "mockloginid";
	private String email = "mock@example.com";
	private String username = "mockuser";
	private String password = "mock1234!";

	public MemberBuilderForTest id(Long id) {
		this.id = id;
		return this;
	}

	public MemberBuilderForTest loginId(String loginId) {
		this.loginId = loginId;
		return this;
	}

	public MemberBuilderForTest email(String email) {
		this.email = email;
		return this;
	}

	public MemberBuilderForTest username(String username) {
		this.username = username;
		return this;
	}

	public MemberBuilderForTest password(String password) {
		this.password = password;
		return this;
	}

	public Member build() {
		Member member = Member.builder()
			.loginId(loginId)
			.email(email)
			.username(username)
			.password(password)
			.build();

		// Set ID using reflection
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}
}
