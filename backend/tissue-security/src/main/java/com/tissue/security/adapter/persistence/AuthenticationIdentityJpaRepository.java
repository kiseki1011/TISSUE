package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface AuthenticationIdentityJpaRepository
        extends Repository<AuthenticationIdentity, Long>, AuthenticationIdentityRepository {

    @Override
    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    @Override
    Optional<AuthenticationIdentity> findByProviderAndIdentifier(AuthenticationProvider provider, String identifier);

    @Override
    boolean existsByProviderAndIdentifier(AuthenticationProvider provider, String identifier);

    @Override
    Optional<AuthenticationIdentity> findByMemberIdAndProvider(Long memberId, AuthenticationProvider provider);
}
