package com.tissue.feature.vcs.domain.support;

import com.tissue.feature.vcs.domain.exception.VcsErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.InternalServerException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    /**
     * Verifies the HMAC SHA256 signature of a webhook payload.
     *
     * @param payload   The raw request body.
     * @param signature The signature from the header.
     *                  (Example: sha256=...)
     * @param secret    The secret configured for the webhook.
     */
    public void verifySignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("Missing or invalid signature header");
            throw new ForbiddenException(VcsErrorCode.INVALID_WEBHOOK_SECRET);
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
            mac.init(secretKeySpec);

            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = "sha256=" + bytesToHex(digest);

            if (!MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Webhook signature mismatch");
                throw new ForbiddenException(VcsErrorCode.INVALID_WEBHOOK_SECRET);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error during webhook signature verification", e);
            throw new InternalServerException(VcsErrorCode.WEBHOOK_PROCESSING_ERROR, e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
