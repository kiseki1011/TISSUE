package com.tissue.security.application.port.oidc;

/**
 * Outbound port to an external OIDC IdP for the OAuth2 Device Authorization Grant (RFC 8628).
 *
 * <p>Tissue brokers the device flow on behalf of the (terminal) client: it asks the IdP for a user code,
 * then polls the IdP's token endpoint. The IdP only authenticates the user; the returned identity is then
 * mapped to a Tissue member and Tissue issues its own session tokens.
 */
public interface OidcClient {

    /**
     * Begins device authorization: returns the user code + verification URI the end user must visit.
     */
    OidcDeviceAuthorization startDeviceAuthorization();

    /**
     * Polls the IdP token endpoint once with the given device code. Returns {@code COMPLETE} with the
     * resolved identity when the user has authorized, or a non-terminal/terminal status otherwise.
     */
    OidcTokenResult pollToken(String deviceCode);
}
