package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface AuthenticationIdentityJpaRepository
        extends Repository<AuthenticationIdentity, Long>, AuthenticationIdentityRepository {

    @Override
    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    @Override
    Optional<AuthenticationIdentity> findByProviderAndIdentifier(
            AuthenticationIdentityProvider provider, String identifier);

    @Override
    boolean existsByProviderAndIdentifier(AuthenticationIdentityProvider provider, String identifier);

    @Override
    Optional<AuthenticationIdentity> findByMemberIdAndProvider(Long memberId, AuthenticationIdentityProvider provider);
}
