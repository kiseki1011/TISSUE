package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.AuthIdentityRepository;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface AuthIdentityJpaRepository extends Repository<AuthenticationIdentity, Long>, AuthIdentityRepository {

    @Override
    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    @Override
    Optional<AuthenticationIdentity> findByProviderAndIdentifier(AuthenticationProvider provider, String identifier);
}
