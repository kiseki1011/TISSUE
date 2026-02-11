package com.tissue.application.port.repository;

import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import java.util.Optional;

public interface AuthIdentityRepository {

    AuthenticationIdentity save(AuthenticationIdentity authenticationIdentity);

    Optional<AuthenticationIdentity> findByProviderAndIdentifier(AuthenticationProvider provider, String identifier);
}
