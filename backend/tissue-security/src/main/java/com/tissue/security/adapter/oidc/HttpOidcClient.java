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
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
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
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
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

    @SuppressWarnings("NullAway.Init")
    private OIDCProviderMetadata metadata;

    @SuppressWarnings("NullAway.Init")
    private IDTokenValidator idTokenValidator;

    public HttpOidcClient(TissueAuthProperties authProperties) {
        this.config = authProperties.getOidc();
        this.clientId = new ClientID(config.getClientId());
        this.clientAuthentication = config.getClientSecret().isBlank()
                ? null
                : new ClientSecretBasic(clientId, new Secret(config.getClientSecret()));

        if (this.clientAuthentication == null) {
            log.warn("OIDC client secret is not set. Tissue is acting as a PUBLIC OAuth2 client. "
                    + "For a server deployment, register a confidential client at the IdP and set "
                    + "TISSUE_AUTH_OIDC_CLIENT_SECRET.");
        }
    }

    @PostConstruct
    void init() {
        this.metadata = fetchMetadata();
        this.idTokenValidator = buildValidator(metadata);
    }

    /**
     * Starts the device authorization for OIDC.
     *
     * <p>For device flow, Google does not use
     * <a href="https://datatracker.ietf.org/doc/html/rfc8628#section-3.2">RFC 8628</a>
     * and returns the legacy {@code verification_url}, where the spec for RFC 8628
     * expects {@code verification_uri}.
     * The problem is, Nimbus parser also expects the RFC 8628 spec, which causes a
     * problem when using {@code DeviceAuthorizationResponse.parse}.
     * To solve this problem, we add {@code verification_uri} and copy the value of
     * {@code verification_url} to it.
     */
    @Override
    public OidcDeviceAuthorization startDeviceAuthorization() {
        OIDCProviderMetadata meta = metadata;
        try {
            Scope scope = new Scope(config.getScopes().toArray(String[]::new));

            DeviceAuthorizationRequest request = clientAuthentication != null
                    ? new DeviceAuthorizationRequest(
                            meta.getDeviceAuthorizationEndpointURI(), clientAuthentication, scope, null)
                    : new DeviceAuthorizationRequest(meta.getDeviceAuthorizationEndpointURI(), clientId, scope);

            HTTPResponse httpResponse = request.toHTTPRequest().send();
            if (!httpResponse.indicatesSuccess()) {
                DeviceAuthorizationResponse error = DeviceAuthorizationResponse.parse(httpResponse);
                throw new IllegalStateException("Device authorization failed: "
                        + error.toErrorResponse().getErrorObject().getCode());
            }

            JSONObject json = httpResponse.getBodyAsJSONObject();
            aliasKey(json, "verification_url", "verification_uri");
            aliasKey(json, "verification_url_complete", "verification_uri_complete");

            DeviceAuthorizationSuccessResponse success = DeviceAuthorizationSuccessResponse.parse(json);
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

    private static void aliasKey(JSONObject json, String from, String to) {
        if (!json.containsKey(to) && json.containsKey(from)) {
            json.put(to, json.get(from));
        }
    }

    @Override
    public OidcTokenResult pollToken(String deviceCode) {
        OIDCProviderMetadata meta = metadata;
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

            IDTokenClaimsSet claims = idTokenValidator.validate(idToken, null);
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

    private OIDCProviderMetadata fetchMetadata() {
        try {
            return OIDCProviderMetadata.resolve(new Issuer(config.getIssuerUri()));
        } catch (IOException | GeneralException e) {
            throw new IllegalStateException("OIDC discovery failed for issuer " + config.getIssuerUri(), e);
        }
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
