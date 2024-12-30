package com.tissue.fixture.repository;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.JobType;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.domain.vo.Name;
import com.tissue.api.security.PasswordEncoder;

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
	 * @param loginId  - 로그인 ID
	 * @param email    - 이메일
	 * @param password - 평문 비밀번호 (암호화)
	 * @return 저장된 Member 객체
	 */
	public Member createAndSaveMember(
		String loginId,
		String email,
		String password
	) {
		Member member = Member.builder()
			.loginId(loginId)
			.email(email)
			.password(passwordEncoder.encode(password))
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.birthDate(LocalDate.of(1995, 1, 1))
			.biography("Im a backend engineer.")
			.jobType(JobType.DEVELOPER)
			.build();
		return memberRepository.save(member);
	}
}
