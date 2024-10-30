package com.uranus.taskmanager.fixture.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;

@Component
@Transactional
public class MemberRespositoryFixture {

	@Autowired
	private MemberRepository memberRepository;

	/**
	 * 멤버를 생성하고 저장합니다.
	 *
	 * @param loginId - 로그인 ID
	 * @param email - 이메일
	 * @param password - 비밀번호 (암호화되지 않은 상태)
	 * @return 저장된 Member 객체
	 */
	public Member createMember(String loginId, String email, String password) {
		Member member = Member.builder()
			.loginId(loginId)
			.email(email)
			.password(password) // 테스트에서는 비밀번호를 미리 암호화하지 않음
			.build();
		return memberRepository.save(member);
	}
}
