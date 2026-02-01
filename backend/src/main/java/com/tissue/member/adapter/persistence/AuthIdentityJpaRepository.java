package com.tissue.member.adapter.persistence;

import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface AuthIdentityJpaRepository extends Repository<AuthIdentity, Long>,
    AuthIdentityRepository {

    @Override
    AuthIdentity save(AuthIdentity authIdentity);

    @Override
    Optional<AuthIdentity> findByProviderAndIdentifier(AuthProvider provider, String identifier);
}
