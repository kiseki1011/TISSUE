package com.tissue.api.security.authentication;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tissue.api.member.infrastructure.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * 스프링 시큐리티가 사용자 정보를 조회할 때 사용하는 서비스
 *
 * 역할:
 * - JWT에서 추출한 사용자 ID로 실제 사용자 정보를 DB에서 조회
 * - Member 엔티티를 MemberUserDetails로 변환
 *
 * 왜 필요한가:
 * - JWT 토큰만으로는 사용자의 현재 상태(활성화/비활성화)를 알 수 없음
 * - 실시간으로 사용자 권한이나 상태가 변경될 수 있음
 * - 스프링 시큐리티와 기존 도메인 로직을 연결하는 브릿지 역할
 */
@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

	// private final MemberReader memberReader;
	private final MemberRepository memberRepository;

	// @Override
	// public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	// 	Member member = memberReader.findMemberByLoginIdOrEmail(username);
	// 	return new MemberUserDetails(member);
	// }

	/**
	 * Find by username(in this case loginId or email) extracted from the JWT token
	 */
	// TODO: Consider using Cache-Based DB Lookup using Redis
	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		return memberRepository.findByLoginIdOrEmail(identifier)
			.map(MemberUserDetails::new)
			.orElseThrow(() -> new UsernameNotFoundException("Member not found for identifier: " + identifier));
	}
}
