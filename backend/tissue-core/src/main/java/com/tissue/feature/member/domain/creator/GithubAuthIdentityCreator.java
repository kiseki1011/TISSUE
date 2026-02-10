package com.tissue.feature.member.domain.creator;

import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import com.tissue.feature.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubAuthIdentityCreator implements AuthIdentityCreator {

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.GITHUB;
    }

    @Override
    public AuthIdentity create(Member member, String identifier, @Nullable String credential) {
        return AuthIdentity.createSocialIdentity(member, AuthProvider.GITHUB, identifier);
    }
}
