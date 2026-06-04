package com.tissue.security.adapter.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationRequest;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationResponse;
import com.nimbusds.oauth2.sdk.device.DeviceAuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.device.DeviceCode;
import com.nimbusds.oauth2.sdk.device.DeviceCodeGrant;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import com.tissue.security.application.dto.OidcUserInfo;
import com.tissue.security.application.port.oidc.OidcClient;
import com.tissue.security.application.port.oidc.OidcDeviceAuthorization;
import com.tissue.security.application.port.oidc.OidcTokenResult;
import com.tissue.security.config.TissueAuthProperties;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.auth.mode", havingValue = "OIDC")
public class HttpOidcClient implements OidcClient {

    private final TissueAuthProperties.Oidc config;
    private final ClientID clientId;

    @Nullable
    private final ClientAuthentication clientAuthentication;

    @Nullable
    private volatile OIDCProviderMetadata metadata;

    @Nullable
    private volatile IDTokenValidator idTokenValidator;

    public HttpOidcClient(TissueAuthProperties authProperties) {
        this.config = authProperties.getOidc();
        this.clientId = new ClientID(config.getClientId());
        this.clientAuthentication = config.getClientSecret().isBlank()
                ? null
                : new ClientSecretBasic(clientId, new Secret(config.getClientSecret()));

        if (this.clientAuthentication == null) {
            log.warn("OIDC client-secret is not set. Tissue is acting as a PUBLIC OAuth2 client. "
                    + "For a server deployment, register a confidential client at the IdP and set "
                    + "TISSUE_AUTH_OIDC_CLIENT_SECRET.");
        }
    }

    @Override
    public OidcDeviceAuthorization startDeviceAuthorization() {
        OIDCProviderMetadata meta = metadata();
        try {
            DeviceAuthorizationRequest request = new DeviceAuthorizationRequest(
                    meta.getDeviceAuthorizationEndpointURI(),
                    clientId,
                    new Scope(config.getScopes().toArray(String[]::new)));

            DeviceAuthorizationResponse response =
                    DeviceAuthorizationResponse.parse(request.toHTTPRequest().send());
            if (!response.indicatesSuccess()) {
                throw new IllegalStateException("Device authorization failed: "
                        + response.toErrorResponse().getErrorObject().getCode());
            }

            DeviceAuthorizationSuccessResponse success = response.toSuccessResponse();
            URI complete = success.getVerificationURIComplete();
            return new OidcDeviceAuthorization(
                    success.getDeviceCode().getValue(),
                    success.getUserCode().getValue(),
                    success.getVerificationURI().toString(),
                    complete == null ? null : complete.toString(),
                    (int) success.getInterval(),
                    (int) success.getLifetime());

        } catch (IOException | ParseException e) {
            throw new IllegalStateException("OIDC device authorization request failed", e);
        }
    }

    @Override
    public OidcTokenResult pollToken(String deviceCode) {
        OIDCProviderMetadata meta = metadata();
        try {
            DeviceCodeGrant grant = new DeviceCodeGrant(new DeviceCode(deviceCode));
            TokenRequest request = clientAuthentication != null
                    ? new TokenRequest(meta.getTokenEndpointURI(), clientAuthentication, grant)
                    : new TokenRequest(meta.getTokenEndpointURI(), clientId, grant);

            TokenResponse response =
                    OIDCTokenResponseParser.parse(request.toHTTPRequest().send());
            if (!response.indicatesSuccess()) {
                return mapError(response.toErrorResponse().getErrorObject().getCode());
            }

            OIDCTokens tokens = ((OIDCTokenResponse) response.toSuccessResponse()).getOIDCTokens();
            JWT idToken = tokens.getIDToken();
            if (idToken == null) {
                throw new IllegalStateException("OIDC token response did not contain an ID token");
            }

            IDTokenClaimsSet claims = idTokenValidator(meta).validate(idToken, null);
            return OidcTokenResult.complete(toUserInfo(claims));

        } catch (IOException | ParseException | JOSEException | BadJOSEException e) {
            throw new IllegalStateException("OIDC token poll failed", e);
        }
    }

    private OidcUserInfo toUserInfo(IDTokenClaimsSet claims) {
        String subject = claims.getSubject().getValue();
        String email = verifiedEmail(claims);
        String username = claimOrNull(claims, config.getUsernameClaim());
        String name = claimOrNull(claims, config.getNameClaim());
        return new OidcUserInfo(subject, email, username, name);
    }

    private @Nullable String verifiedEmail(IDTokenClaimsSet claims) {
        String email = claims.getStringClaim(config.getEmailClaim());
        if (email == null || email.isBlank()) {
            return null;
        }
        Object verified = claims.getClaim("email_verified");
        boolean unverified = (verified instanceof Boolean flag && !flag)
                || (verified instanceof String text && text.equalsIgnoreCase("false"));
        return unverified ? null : email;
    }

    /**
     * Reads a string claim, normalizing absent or blank values to {@code null} (mirroring {@link #verifiedEmail}).
     * Username/name policy — fallbacks — is applied later by the resolver, where a verified email is guaranteed.
     */
    private @Nullable String claimOrNull(IDTokenClaimsSet claims, String claimName) {
        String value = claims.getStringClaim(claimName);
        return (value == null || value.isBlank()) ? null : value;
    }

    private static OidcTokenResult mapError(@Nullable String code) {
        return switch (code == null ? "" : code) {
            case "authorization_pending" -> OidcTokenResult.pending();
            case "slow_down" -> OidcTokenResult.of(OidcTokenResult.Status.SLOW_DOWN);
            case "access_denied" -> OidcTokenResult.of(OidcTokenResult.Status.DENIED);
            case "expired_token" -> OidcTokenResult.of(OidcTokenResult.Status.EXPIRED);
            default -> OidcTokenResult.of(OidcTokenResult.Status.ERROR);
        };
    }

    private OIDCProviderMetadata metadata() {
        OIDCProviderMetadata local = metadata;
        if (local == null) {
            synchronized (this) {
                local = metadata;
                if (local == null) {
                    local = fetchMetadata();
                    metadata = local;
                }
            }
        }
        return local;
    }

    private OIDCProviderMetadata fetchMetadata() {
        try {
            return OIDCProviderMetadata.resolve(new Issuer(config.getIssuerUri()));
        } catch (IOException | GeneralException e) {
            throw new IllegalStateException("OIDC discovery failed for issuer " + config.getIssuerUri(), e);
        }
    }

    private IDTokenValidator idTokenValidator(OIDCProviderMetadata meta) {
        IDTokenValidator local = idTokenValidator;
        if (local == null) {
            synchronized (this) {
                local = idTokenValidator;
                if (local == null) {
                    local = buildValidator(meta);
                    idTokenValidator = local;
                }
            }
        }
        return local;
    }

    private IDTokenValidator buildValidator(OIDCProviderMetadata meta) {
        try {
            return new IDTokenValidator(
                    meta.getIssuer(),
                    clientId,
                    JWSAlgorithm.RS256,
                    meta.getJWKSetURI().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid JWKS URI from IdP", e);
        }
    }
}
