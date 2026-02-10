package com.tissue.feature.member.domain.creator;

import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import com.tissue.feature.member.domain.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthIdentityManager {

    private final List<AuthIdentityCreator> creators;

    public AuthIdentity create(Member member, AuthProvider provider, String identifier, @Nullable String credential) {
        return creators.stream()
                .filter(creator -> creator.supports(provider))
                .findFirst()
                .map(creator -> creator.create(member, identifier, credential))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported AuthProvider: " + provider));
    }
}
