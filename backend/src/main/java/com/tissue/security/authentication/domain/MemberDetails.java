package com.tissue.security.authentication.domain;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.security.authorization.SystemRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Spring Security에서 사용하는 사용자 정보 객체입니다.
 * <p>
 * Member 도메인 객체와 Spring Security의 UserDetails 인터페이스 사이의 어댑터 역할을 합니다.
 * 비밀번호는 Member 엔티티가 아닌 AuthIdentity에서 가져온 값을 사용합니다.
 * </p>
 */
@Getter
public class MemberDetails implements UserDetails, OAuth2User {

    private final Long memberId;
    private final String email;
    private final String name;
    private final String nickname;

    @Nullable
    private final String password;

    private final SystemRole role;
    private final MemberStatus status;

    private final Collection<? extends GrantedAuthority> authorities;

    @Nullable
    private Map<String, Object> attributes;

    private boolean elevated;

    /**
     * Member 객체와 비밀번호를 받아 UserDetails를 생성 (Form Login 용)
     *
     * @param member 회원 정보
     * @param password 암호화된 비밀번호
     */
    public MemberDetails(Member member, @Nullable String password) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.nickname = member.getUsername();
        this.password = password;
        this.role = member.getRole();
        this.status = member.getStatus();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()));
    }

    /**
     * Member 객체와 OAuth2 속성을 받아 UserDetails를 생성 (OAuth2 Login 용)
     *
     * @param member 회원 정보
     * @param attributes OAuth2 속성
     */
    public MemberDetails(Member member, Map<String, Object> attributes) {
        this(member, (String) null);
        this.attributes = attributes;
    }

    public void setElevated(boolean elevated) {
        this.elevated = elevated;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (elevated) {
            List<GrantedAuthority> newAuthorities = new ArrayList<>(authorities);
            newAuthorities.add(new SimpleGrantedAuthority("ELEVATED"));
            return newAuthorities;
        }
        return authorities;
    }

    @Override
    public @Nullable Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != MemberStatus.LOCKED;
    }

    @Override
    public boolean isEnabled() {
        return this.status != MemberStatus.DELETED;
    }
}
