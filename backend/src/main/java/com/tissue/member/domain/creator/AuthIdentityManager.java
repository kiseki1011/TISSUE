package com.tissue.member.domain.creator;

import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthIdentityManager {

    private final List<AuthIdentityCreator> creators;

    public AuthIdentity create(Member member, AuthProvider provider, String identifier, String credential) {
        return creators.stream()
                .filter(creator -> creator.supports(provider))
                .findFirst()
                .map(creator -> creator.create(member, identifier, credential))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported AuthProvider: " + provider));
    }
}
