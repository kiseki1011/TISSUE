package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface AuthenticationIdentityJpaRepository
        extends Repository<AuthenticationIdentity, Long>, AuthenticationIdentityRepository {

    @Override
    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    @Override
    Optional<AuthenticationIdentity> findByProviderAndIdentifier(AuthenticationProvider provider, String identifier);

    @Override
    Optional<AuthenticationIdentity> findByMemberIdAndProvider(Long memberId, AuthenticationProvider provider);
}
