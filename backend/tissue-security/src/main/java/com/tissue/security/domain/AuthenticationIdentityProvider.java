package com.tissue.security.domain;

import java.util.Locale;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public enum AuthenticationIdentityProvider {
    EMAIL,
    USERNAME,
    GOOGLE,
    GITHUB;

    public static AuthenticationIdentityProvider fromRegistrationId(String registrationId) {
        try {
            return valueOf(registrationId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }
    }
}
