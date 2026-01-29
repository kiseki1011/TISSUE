package com.tissue.global.security.principal;

import com.tissue.global.security.SystemRole;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
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
     * Use Member entity and OAuth2 attributes to create MemberDetails (For OAuth2 Login)
     *
     * @param attributes OAuth2 attributes
     */
    // TODO: Consider making this into a static factory method
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
