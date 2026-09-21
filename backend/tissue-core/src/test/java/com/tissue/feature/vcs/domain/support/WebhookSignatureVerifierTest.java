package com.tissue.feature.vcs.domain.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.vcs.domain.exception.VcsErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

    private static final String SECRET = "webhook-secret";
    private static final String PAYLOAD = "{\"ref\":\"refs/heads/main\"}";

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    private static String sign(String payload, String secret) {
        return "sha256=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(payload);
    }

    @Test
    void acceptsACorrectlySignedPayload() {
        // given
        String signature = sign(PAYLOAD, SECRET);

        // when & then
        assertThatCode(() -> verifier.verifySignature(PAYLOAD, signature, SECRET))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsATamperedPayload() {
        // given - signature is valid for PAYLOAD, but a different body is given
        String signature = sign(PAYLOAD, SECRET);

        // when & then
        assertThatThrownBy(() -> verifier.verifySignature(PAYLOAD + "-tampered", signature, SECRET))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(VcsErrorCode.INVALID_WEBHOOK_SECRET);
    }

    @Test
    void rejectsAMissingSignatureHeader() {
        // when & then
        assertThatThrownBy(() -> verifier.verifySignature(PAYLOAD, null, SECRET))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(VcsErrorCode.MISSING_SIGNATURE);
    }

    @Test
    void rejectsASignatureWithoutTheExpectedPrefix() {
        // given - correct digest but stripped of the "sha256=" prefix
        String digestOnly = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, SECRET).hmacHex(PAYLOAD);

        // when & then
        assertThatThrownBy(() -> verifier.verifySignature(PAYLOAD, digestOnly, SECRET))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(VcsErrorCode.INVALID_WEBHOOK_SECRET);
    }
}
