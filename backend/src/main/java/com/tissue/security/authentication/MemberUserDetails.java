package com.tissue.security.authentication;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.security.authorization.SystemRole;

import lombok.Getter;

/**
 * Member 엔티티를 스프링 시큐리티의 UserDetails로 변환하는 어댑터
 * <p>
 * 역할:
 * - 도메인 모델(Member)과 스프링 시큐리티 사이의 브릿지
 * - 스프링 시큐리티가 이해할 수 있는 형태로 사용자 정보 제공
 */
@Getter
public class MemberUserDetails implements UserDetails {

	private final Long memberId;
	private final String email;
	private final String username;
	private final String password;
	private final SystemRole role;
	private final MemberStatus status;

	private final Collection<? extends GrantedAuthority> authorities;

	private boolean elevated;

	public MemberUserDetails(Member member) {
		this.memberId = member.getId();
		this.email = member.getEmail();
		this.username = member.getUsername();
		this.password = member.getPassword();
		this.role = member.getRole();
		this.status = member.getStatus();

		this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()));
	}

	public void setElevated(boolean elevated) {
		this.elevated = elevated;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return this.status != MemberStatus.LOCKED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.status != MemberStatus.DELETED;
	}
}
