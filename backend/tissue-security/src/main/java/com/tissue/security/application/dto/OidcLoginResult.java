package com.tissue.security.application.dto;

import com.tissue.security.application.port.oidc.OidcTokenResult;
import org.jspecify.annotations.Nullable;

/**
 * Result of an OIDC device login poll.
 *
 * <p>Either still in progress / failed (no tokens), or
 * {@code COMPLETE} with freshly issued Tissue access / refresh tokens.
 */
public record OidcLoginResult(
        OidcTokenResult.Status status, @Nullable TokenPair tokens) {

    public static OidcLoginResult complete(TokenPair tokens) {
        return new OidcLoginResult(OidcTokenResult.Status.COMPLETE, tokens);
    }

    public static OidcLoginResult of(OidcTokenResult.Status status) {
        return new OidcLoginResult(status, null);
    }
}
