package com.tissue.security.application.port.oidc;

public interface OidcClient {

    /**
     * Begins device authorization.
     *
     * <p>Returns the user code + verification URI the end user must visit.
     */
    OidcDeviceAuthorization startDeviceAuthorization();

    /**
     * Polls the IdP token endpoint once with the given device code.
     *
     * <p>Returns {@code COMPLETE} with the resolved identity when the user has authorized,
     * or a non-terminal/terminal status otherwise.
     */
    OidcTokenResult pollToken(String deviceCode);
}
