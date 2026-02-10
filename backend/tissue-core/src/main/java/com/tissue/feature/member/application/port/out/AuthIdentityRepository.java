package com.tissue.feature.member.application.port.out;

import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import java.util.Optional;

public interface AuthIdentityRepository {

    AuthIdentity save(AuthIdentity authIdentity);

    Optional<AuthIdentity> findByProviderAndIdentifier(AuthProvider provider, String identifier);
}
