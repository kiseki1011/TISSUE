package com.tissue.member.domain.creator;

import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleAuthIdentityCreator implements AuthIdentityCreator {

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.GOOGLE;
    }

    @Override
    public AuthIdentity create(Member member, String identifier, @Nullable String credential) {
        return AuthIdentity.createSocialIdentity(member, AuthProvider.GOOGLE, identifier);
    }
}
