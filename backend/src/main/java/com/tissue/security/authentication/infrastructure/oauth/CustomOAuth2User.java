package com.tissue.security.authentication.infrastructure.oauth;

import com.tissue.member.domain.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class CustomOAuth2User implements OAuth2User {

    @Nullable
    private final Member member;

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String provider;
    private final String identifier;
    private final String email;

    public CustomOAuth2User(
            @Nullable Member member,
            Map<String, Object> attributes,
            String nameAttributeKey,
            String provider,
            String identifier,
            String email) {

        this.member = member;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.provider = provider;
        this.identifier = identifier;
        this.email = email;
    }

    @Override
    public Map<String, Object> getAttributes() {

        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (member == null) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"));
        }
        return Collections.singletonList(
                new SimpleGrantedAuthority(member.getRole().getAuthority()));
    }

    @Override
    public String getName() {
        return member != null ? member.getId().toString() : identifier;
    }
}
