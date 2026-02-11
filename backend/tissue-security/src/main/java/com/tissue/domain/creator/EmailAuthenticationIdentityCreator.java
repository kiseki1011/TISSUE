package com.tissue.domain.creator;

import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.feature.member.domain.Member;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class EmailAuthenticationIdentityCreator implements AuthenticationIdentityCreator {

    @Override
    public boolean supports(AuthenticationProvider provider) {
        return provider == AuthenticationProvider.EMAIL;
    }

    @Override
    public AuthenticationIdentity create(Member member, String identifier, @Nullable String credential) {
        if (credential == null) {
            throw new IllegalArgumentException("Password credential is required for EMAIL provider");
        }
        return AuthenticationIdentity.createEmailIdentity(member, identifier, credential);
    }
}
