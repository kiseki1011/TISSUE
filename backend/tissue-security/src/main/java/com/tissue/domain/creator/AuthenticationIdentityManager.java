package com.tissue.domain.creator;

import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.feature.member.domain.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationIdentityManager {

    private final List<AuthenticationIdentityCreator> creators;

    public AuthenticationIdentity create(
            Member member, AuthenticationProvider provider, String identifier, @Nullable String credential) {
        return creators.stream()
                .filter(creator -> creator.supports(provider))
                .findFirst()
                .map(creator -> creator.create(member, identifier, credential))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported AuthProvider: " + provider));
    }
}
