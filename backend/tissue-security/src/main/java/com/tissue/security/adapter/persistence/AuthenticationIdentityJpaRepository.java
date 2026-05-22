package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface AuthenticationIdentityJpaRepository
        extends Repository<AuthenticationIdentity, Long>, AuthenticationIdentityRepository {

    @Override
    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    @Override
    @Query("SELECT ai FROM AuthenticationIdentity ai JOIN FETCH ai.member"
            + " WHERE ai.provider = :provider AND ai.identifier = :identifier")
    Optional<AuthenticationIdentity> findByProviderAndIdentifier(
            AuthenticationIdentityProvider provider, String identifier);

    @Override
    boolean existsByProviderAndIdentifier(AuthenticationIdentityProvider provider, String identifier);

    @Override
    Optional<AuthenticationIdentity> findByMemberIdAndProvider(Long memberId, AuthenticationIdentityProvider provider);

    @Override
    List<AuthenticationIdentity> findAllByMemberIdAndProviderIn(
            Long memberId, List<AuthenticationIdentityProvider> providers);

    @Override
    void deleteByMemberId(Long memberId);
}
