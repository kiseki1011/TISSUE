package com.tissue.global.security.oauth2;

import com.tissue.global.security.oauth2.userinfo.OAuth2UserInfo;
import com.tissue.member.domain.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    @Nullable
    private final Member member;

    private final OAuth2UserInfo userInfo;

    @Override
    public Map<String, Object> getAttributes() {
        return userInfo.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (member != null) {
            return Collections.singleton(
                    new SimpleGrantedAuthority(member.getRole().getAuthority()));
        }
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @Override
    public String getName() {
        String email = userInfo.getEmail();
        return email != null ? email : userInfo.getProviderId();
    }

    public boolean isRegistered() {
        return member != null;
    }
}
