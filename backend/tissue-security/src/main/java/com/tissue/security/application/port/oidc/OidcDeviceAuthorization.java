package com.tissue.security.application.port.oidc;

import org.jspecify.annotations.Nullable;

/**
 * The IdP's response to a device authorization request
 *
 * <p> <a href=https://datatracker.ietf.org/doc/html/rfc8628#section-3.2>RFC 8628 #3.2</a>
 *
 * @param deviceCode               the code Tissue polls the token endpoint with (relayed to the client)
 * @param userCode                 the short code the user types at the verification URI
 * @param verificationUri          where the user authenticates
 * @param verificationUriComplete  optional URI embedding the user code
 * @param interval                 minimum seconds between polls
 * @param expiresIn                seconds until the device code expires
 */
public record OidcDeviceAuthorization(
        String deviceCode,
        String userCode,
        String verificationUri,
        @Nullable String verificationUriComplete,
        int interval,
        int expiresIn) {}
