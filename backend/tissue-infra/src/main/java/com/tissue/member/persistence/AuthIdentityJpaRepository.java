package com.tissue.member.persistence;

import com.tissue.feature.member.application.port.out.AuthIdentityRepository;
import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface AuthIdentityJpaRepository extends Repository<AuthIdentity, Long>, AuthIdentityRepository {

    @Override
    AuthIdentity save(AuthIdentity authIdentity);

    @Override
    Optional<AuthIdentity> findByProviderAndIdentifier(AuthProvider provider, String identifier);
}
