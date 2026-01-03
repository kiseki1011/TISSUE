package com.tissue.member.application.port.out;

import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import java.util.Optional;

// TODO: just use Repository(rely on spring data)
public interface AuthIdentityRepository {

    AuthIdentity save(AuthIdentity authIdentity);

    Optional<AuthIdentity> findByProviderAndIdentifier(AuthProvider provider, String identifier);
}
