package com.tissue.feature.member.domain.creator;

import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import com.tissue.feature.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailAuthIdentityCreator implements AuthIdentityCreator {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.EMAIL;
    }

    @Override
    public AuthIdentity create(Member member, String identifier, @Nullable String credential) {
        if (credential == null) {
            throw new IllegalArgumentException("Password credential is required for EMAIL provider");
        }
        return AuthIdentity.createEmailIdentity(member, identifier, passwordEncoder.encode(credential));
    }
}
