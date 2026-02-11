package com.tissue.domain.creator;

import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.feature.member.domain.Member;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class GoogleAuthenticationIdentityCreator implements AuthenticationIdentityCreator {

    @Override
    public boolean supports(AuthenticationProvider provider) {
        return provider == AuthenticationProvider.GOOGLE;
    }

    @Override
    public AuthenticationIdentity create(Member member, String identifier, @Nullable String credential) {
        return AuthenticationIdentity.createSocialIdentity(member, AuthenticationProvider.GOOGLE, identifier);
    }
}
