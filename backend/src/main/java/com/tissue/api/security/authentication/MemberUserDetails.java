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

		this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()));
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	/**
	 * JWT does not use passwords
	 * Return null or "" to implement UserDetails interface
	 */
	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		// identifier that spring security uses internally (logging, etc...)
		return loginId;
	}

	@Override
	public boolean isAccountNonExpired() {
		// user a seperate field to check account expiration in Member
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// use if account lock is needed
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// use if you need credential expiration
		return true;
	}

	@Override
	public boolean isEnabled() {
		// use if you need account activation status (probably can use for Member soft delete)
		return true;
	}
}
