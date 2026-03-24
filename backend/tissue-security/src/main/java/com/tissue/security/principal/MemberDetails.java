package com.tissue.security.principal;

import com.tissue.feature.member.domain.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MemberDetails implements UserDetails {

    @Getter
    private final Long memberId;

    @Getter
    private final String email;

    @Getter
    private final String handle;

    @Nullable
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    private boolean elevated;

    /**
     * For initial login
     */
    public MemberDetails(Member member, @Nullable String password) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.handle = member.getUsername();
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority(member.getRole().getAuthority()));
    }

    /**
     * For stateless token-based authentication (reconstructed from JWT)
     */
    public MemberDetails(
            Long memberId, String email, String username, Collection<? extends GrantedAuthority> authorities) {
        this.memberId = memberId;
        this.email = email;
        this.handle = username;
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

    /**
     * Returns the email used for authentication, not the username.
     * Use {@link #getHandle()} for the actual username.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }
}
