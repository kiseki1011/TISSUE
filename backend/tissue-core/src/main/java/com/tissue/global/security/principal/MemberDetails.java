package com.tissue.global.security.principal;

import com.tissue.member.domain.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Principal object representing the authenticated member.
 * Designed to be technology-agnostic and work with both DB-based and Token-based authentication.
 */
@Getter
public class MemberDetails implements UserDetails, OAuth2User {

    private final Long memberId;
    private final String email;
    private final String nickname;

    @Nullable
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    @Nullable
    private Map<String, Object> attributes;

    private boolean elevated;

    /**
     * Constructor for initial login or DB-based authentication.
     */
    public MemberDetails(Member member, @Nullable String password) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.nickname = member.getName();
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority(member.getRole().getAuthority()));
    }

    /**
     * Constructor for stateless token-based authentication (reconstructed from JWT).
     */
    public MemberDetails(Long memberId, String email, String nickname, Collection<? extends GrantedAuthority> authorities) {
        this.memberId = memberId;
        this.email = email;
        this.nickname = nickname;
        this.authorities = authorities;
        this.password = null;
    }

    public void grantElevated(boolean elevated) {
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
        return nickname;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Simplified for Stateless
    }

    @Override
    public boolean isEnabled() {
        return true; // Simplified for Stateless
    }
}
