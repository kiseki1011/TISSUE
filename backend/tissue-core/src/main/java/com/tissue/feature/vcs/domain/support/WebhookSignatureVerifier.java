package com.tissue.feature.vcs.domain.support;

import com.tissue.feature.vcs.domain.exception.VcsErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebhookSignatureVerifier {

    private static final String SIGNATURE_PREFIX = "sha256=";

    /**
     * Verifies the HMAC SHA256 signature.
     *
     * @param payload   The raw request body.
     * @param signature The signature from the header. (Example: sha256=...)
     * @param secret    The secret configured for the webhook.
     */
    public void verifySignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Missing or invalid signature header");
            throw new ForbiddenException(VcsErrorCode.INVALID_WEBHOOK_SECRET);
        }

        String computedSignature = SIGNATURE_PREFIX + new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(payload);

        if (!MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Webhook signature mismatch");
            throw new ForbiddenException(VcsErrorCode.INVALID_WEBHOOK_SECRET);
        }
    }
}
