package com.tissue.security.adapter.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tissue.security.application.dto.OidcUserInfo;
import com.tissue.security.application.port.oidc.OidcDeviceAuthorization;
import com.tissue.security.application.port.oidc.OidcTokenResult;
import com.tissue.security.config.TissueAuthProperties;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "Check OIDC device authorization spec (RFC8628) and google spec.")
class HttpOidcClientTest {

    private static final String CLIENT_ID = "tissue-client";
    private static final String KEY_ID = "test-key";

    private static WireMockServer wireMock;
    private static RSAKey rsaKey;
    private static String issuer;
    private static HttpOidcClient client;

    @BeforeAll
    static void beforeAll() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();

        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        issuer = "http://localhost:" + wireMock.port();

        String discovery = """
                {
                  "issuer": "%s",
                  "authorization_endpoint": "%s/authorize",
                  "token_endpoint": "%s/token",
                  "device_authorization_endpoint": "%s/device",
                  "jwks_uri": "%s/jwks",
                  "response_types_supported": ["code"],
                  "subject_types_supported": ["public"],
                  "id_token_signing_alg_values_supported": ["RS256"]
                }""".formatted(issuer, issuer, issuer, issuer, issuer);
        wireMock.stubFor(get(urlEqualTo("/.well-known/openid-configuration")).willReturn(okJson(discovery)));
        wireMock.stubFor(get(urlEqualTo("/jwks")).willReturn(okJson(new JWKSet(rsaKey.toPublicJWK()).toString())));

        TissueAuthProperties properties = new TissueAuthProperties();
        properties.getOidc().setIssuerUri(issuer);
        properties.getOidc().setClientId(CLIENT_ID);
        client = new HttpOidcClient(properties);
        client.init(); // needs to invoke manually cause its @PostConstruct
    }

    @AfterAll
    static void afterAll() {
        wireMock.stop();
    }

    @Test
    @DisplayName("success: device authorization returns the user code and device code")
    void startDeviceAuthorization() {
        String response = """
                {"device_code":"DC-1","user_code":"WDJB-MJHT","verification_uri":"%s/device","interval":5,"expires_in":600}""".formatted(issuer);
        wireMock.stubFor(post(urlEqualTo("/device")).willReturn(okJson(response)));

        OidcDeviceAuthorization result = client.startDeviceAuthorization();

        assertThat(result.deviceCode()).isEqualTo("DC-1");
        assertThat(result.userCode()).isEqualTo("WDJB-MJHT");
        assertThat(result.interval()).isEqualTo(5);
        assertThat(result.expiresIn()).isEqualTo(600);
    }

    @Test
    @DisplayName("google's device auth response verification_url is accepted as verification_uri")
    void startDeviceAuthorizationGoogleVerificationUrl() {
        String response = """
                {"device_code":"DC-G","user_code":"GOOG-CODE","verification_url":"https://www.google.com/device","interval":5,"expires_in":600}""";
        wireMock.stubFor(post(urlEqualTo("/device")).willReturn(okJson(response)));

        OidcDeviceAuthorization result = client.startDeviceAuthorization();

        assertThat(result.userCode()).isEqualTo("GOOG-CODE");
        assertThat(result.verificationUri()).isEqualTo("https://www.google.com/device");
    }

    @Test
    @DisplayName("success: a completed authorization yields the validated identity from the ID token")
    void pollTokenComplete() throws Exception {
        String idToken = signIdToken(baseClaims()
                .claim("email", "gildong@company.com")
                .claim("email_verified", true)
                .claim("preferred_username", "gildong")
                .claim("name", "Gildong")
                .build());
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson(
                        "{\"access_token\":\"a\",\"token_type\":\"Bearer\",\"id_token\":\"%s\"}".formatted(idToken))));

        OidcTokenResult result = client.pollToken("DC-1");

        assertThat(result.status()).isEqualTo(OidcTokenResult.Status.COMPLETE);
        OidcUserInfo info = Objects.requireNonNull(result.userInfo());
        assertThat(info.subject()).isEqualTo("sub-1");
        assertThat(info.email()).isEqualTo("gildong@company.com");
        assertThat(info.username()).isEqualTo("gildong");
        assertThat(info.name()).isEqualTo("Gildong");
    }

    @Test
    @DisplayName("pending: authorization_pending maps to PENDING with no identity")
    void pollTokenPending() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"authorization_pending\"}")));

        OidcTokenResult result = client.pollToken("DC-1");

        assertThat(result.status()).isEqualTo(OidcTokenResult.Status.PENDING);
        assertThat(result.userInfo()).isNull();
    }

    @Test
    @DisplayName("normalize: a missing username claim is null (the resolver derives the final username)")
    void missingUsernameClaimIsNull() throws Exception {
        String idToken = signIdToken(baseClaims()
                .subject("sub-2")
                .claim("email", "no.username@company.com")
                .claim("email_verified", true)
                .build());
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson(
                        "{\"access_token\":\"a\",\"token_type\":\"Bearer\",\"id_token\":\"%s\"}".formatted(idToken))));

        OidcTokenResult result = client.pollToken("DC-1");

        OidcUserInfo info = Objects.requireNonNull(result.userInfo());
        assertThat(info.username()).isNull();
        assertThat(info.email()).isEqualTo("no.username@company.com");
    }

    @Test
    @DisplayName("security: an explicitly unverified email is dropped (becomes null)")
    void unverifiedEmailDropped() throws Exception {
        String idToken = signIdToken(baseClaims()
                .subject("sub-3")
                .claim("email", "spoof@company.com")
                .claim("email_verified", false)
                .claim("preferred_username", "spoof")
                .build());
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson(
                        "{\"access_token\":\"a\",\"token_type\":\"Bearer\",\"id_token\":\"%s\"}".formatted(idToken))));

        OidcTokenResult result = client.pollToken("DC-1");

        OidcUserInfo info = Objects.requireNonNull(result.userInfo());
        assertThat(info.email()).isNull();
        assertThat(info.username()).isEqualTo("spoof");
    }

    @Test
    @DisplayName("normalize: a blank name claim is dropped to null so the resolver can fall back")
    void blankNameDropped() throws Exception {
        String idToken = signIdToken(baseClaims()
                .subject("sub-4")
                .claim("email", "blankname@company.com")
                .claim("email_verified", true)
                .claim("preferred_username", "blankname")
                .claim("name", "   ")
                .build());
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson(
                        "{\"access_token\":\"a\",\"token_type\":\"Bearer\",\"id_token\":\"%s\"}".formatted(idToken))));

        OidcTokenResult result = client.pollToken("DC-1");

        OidcUserInfo info = Objects.requireNonNull(result.userInfo());
        assertThat(info.name()).isNull();
        assertThat(info.username()).isEqualTo("blankname");
    }

    private static JWTClaimsSet.Builder baseClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("sub-1")
                .audience(CLIENT_ID)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)));
    }

    private static String signIdToken(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
        jwt.sign(new RSASSASigner(rsaKey));
        return jwt.serialize();
    }
}
