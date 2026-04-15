package com.tissue.security.oauth2;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.oauth2.userinfo.OAuth2UserInfo;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
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
            return Set.of(new SimpleGrantedAuthority(member.getRole().getAuthority()));
        }
        return Set.of(new SimpleGrantedAuthority("ROLE_GUEST"));
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
