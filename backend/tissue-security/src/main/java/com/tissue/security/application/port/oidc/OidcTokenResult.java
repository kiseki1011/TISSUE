package com.tissue.security.application.port.oidc;

import com.tissue.security.application.dto.OidcUserInfo;
import org.jspecify.annotations.Nullable;

/**
 * The outcome of polling the IdP token endpoint.
 *
 * <ul>
 *   <li>{@code COMPLETE} — user authorized; {@link #userInfo()} holds the validated identity.
 *   <li>{@code PENDING} — user has not finished yet; keep polling.
 *   <li>{@code SLOW_DOWN} — poll less frequently.
 *   <li>{@code DENIED} — user (or IdP) denied the request.
 *   <li>{@code EXPIRED} — the device code expired; restart the flow.
 *   <li>{@code ERROR} — any other token-endpoint error.
 * </ul>
 */
public record OidcTokenResult(Status status, @Nullable OidcUserInfo userInfo) {

    public enum Status {
        COMPLETE,
        PENDING,
        SLOW_DOWN,
        DENIED,
        EXPIRED,
        ERROR
    }

    public static OidcTokenResult complete(OidcUserInfo userInfo) {
        return new OidcTokenResult(Status.COMPLETE, userInfo);
    }

    public static OidcTokenResult pending() {
        return new OidcTokenResult(Status.PENDING, null);
    }

    public static OidcTokenResult of(Status status) {
        return new OidcTokenResult(status, null);
    }
}
