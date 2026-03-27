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
    @Nullable
    private final String email;

    private final String username;

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
        this.username = member.getUsername();
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority(member.getRole().getAuthority()));
    }

    /**
     * For stateless token-based authentication (reconstructed from JWT)
     */
    public MemberDetails(
            Long memberId,
            @Nullable String email,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        this.memberId = memberId;
        this.email = email;
        this.username = username;
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
    public String getUsername() {
        return username;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }
}
