package com.tissue.security.application.port.repository;

import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationProvider;
import java.util.Optional;

public interface AuthenticationIdentityRepository {

    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    Optional<AuthenticationIdentity> findByProviderAndIdentifier(AuthenticationProvider provider, String identifier);

    boolean existsByProviderAndIdentifier(AuthenticationProvider provider, String identifier);

    Optional<AuthenticationIdentity> findByMemberIdAndProvider(Long memberId, AuthenticationProvider provider);
}
