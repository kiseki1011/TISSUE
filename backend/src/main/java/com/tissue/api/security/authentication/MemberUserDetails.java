package com.tissue.api.security.authentication;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.security.authorization.enums.SystemRole;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Member 엔티티를 스프링 시큐리티의 UserDetails로 변환하는 어댑터
 *
 * 역할:
 * - 도메인 모델(Member)과 스프링 시큐리티 사이의 브릿지
 * - 스프링 시큐리티가 이해할 수 있는 형태로 사용자 정보 제공
 */
@Getter
@RequiredArgsConstructor
public class MemberUserDetails implements UserDetails {

	private final Long memberId;
	private final String loginId;
	private final String email;
	private final String username;
	private final SystemRole role;

	private final Collection<? extends GrantedAuthority> authorities;

	public MemberUserDetails(Member member) {
		this.memberId = member.getId();
		this.loginId = member.getLoginId();
		this.email = member.getEmail();
		this.username = member.getUsername();
		this.role = member.getRole();

		// Spring Security에 맞는 권한 객체로 변환
		this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()));
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	/**
	 * JWT 기반 인증에선 비밀번호를 사용하지 않음.
	 * 단, UserDetails 인터페이스 구현을 위해 null 또는 암호화된 비밀번호 반환
	 */
	@Override
	public String getPassword() {
		return null; // "****" 또는 "PROTECTED" 반환할까?
	}

	@Override
	public String getUsername() {
		// Security 내부적으로 사용하는 식별자 (로그 추적 등)
		return loginId;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true; // 실무에서는 만료 여부를 별도 필드로 체크
	}

	@Override
	public boolean isAccountNonLocked() {
		return true; // 잠금 여부가 있다면 해당 필드 체크
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true; // 자격 증명 만료 여부 (ex. 비밀번호 유효기간)
	}

	@Override
	public boolean isEnabled() {
		return true; // 탈퇴, 비활성화 여부
	}
}
