package com.tissue.security.application.port.repository;

import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import java.util.List;
import java.util.Optional;

public interface AuthenticationIdentityRepository {

    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    Optional<AuthenticationIdentity> findByProviderAndIdentifier(
            AuthenticationIdentityProvider provider, String identifier);

    boolean existsByProviderAndIdentifier(AuthenticationIdentityProvider provider, String identifier);

    Optional<AuthenticationIdentity> findByMemberIdAndProvider(Long memberId, AuthenticationIdentityProvider provider);

    List<AuthenticationIdentity> findAllByMemberIdAndProviderIn(
            Long memberId, List<AuthenticationIdentityProvider> providers);
}
