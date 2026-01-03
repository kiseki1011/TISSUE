package com.tissue.security.authentication;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.security.authorization.SystemRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class MemberUserDetails implements UserDetails {

    private final Long memberId;
    private final String email;
    private final String nickname;
    private final String password;
    private final SystemRole role;
    private final MemberStatus status;

    private final Collection<? extends GrantedAuthority> authorities;

    private boolean elevated;

    public MemberUserDetails(Member member) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.nickname = member.getUsername();
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
        if (elevated) {
            List<GrantedAuthority> newAuthorities = new ArrayList<>(authorities);
            newAuthorities.add(new SimpleGrantedAuthority("ELEVATED"));
            return newAuthorities;
        }
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
    public boolean isAccountNonLocked() {
        return this.status != MemberStatus.LOCKED;
    }

    @Override
    public boolean isEnabled() {
        return this.status != MemberStatus.DELETED;
    }
}
