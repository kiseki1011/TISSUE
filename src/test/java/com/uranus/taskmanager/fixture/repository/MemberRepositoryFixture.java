package com.uranus.taskmanager.fixture.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;

@Component
@Transactional
public class MemberRepositoryFixture {

	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * 멤버를 생성하고 저장합니다.
	 *
	 * @param loginId - 로그인 ID
	 * @param email - 이메일
	 * @param password - 비밀번호 (암호화)
	 * @return 저장된 Member 객체
	 */
	public Member createMember(String loginId, String email, String password) {
		Member member = Member.builder()
			.loginId(loginId)
			.email(email)
			.password(passwordEncoder.encode(password))
			.build();
		return memberRepository.save(member);
	}
}
