package com.tissue.vcs.adapter.in.web.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.vcs.application.port.in.GitProviderUseCase;
import com.tissue.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class GithubWebhook {

    private final GitProviderUseCase gitProviderUseCase;
    private final WorkspaceVcsIntegrationRepository vcsIntegrationRepository;
    private final ObjectMapper objectMapper;

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    @PostMapping("/{workspaceKey}/github/webhook")
    public ResponseEntity<Void> handleGithubWebhook(
            @PathVariable String workspaceKey,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestBody String rawPayload) {

        log.info("Received GitHub webhook for workspace: {}", workspaceKey);

        WorkspaceVcsIntegration integration = vcsIntegrationRepository
                .findByWorkspaceKey(workspaceKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integration not found"));

        verifySignature(rawPayload, signature, integration.getWebhookSecret());

        try {
            GithubPrPayload payload = objectMapper.readValue(rawPayload, GithubPrPayload.class);
            if (payload.getPullRequest() != null) {
                gitProviderUseCase.handlePullRequest(payload.toDomainDto(workspaceKey));
            } else {
                log.debug("Ignored non-PR event");
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse GitHub payload", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload");
        }

        return ResponseEntity.ok().build();
    }

    private void verifySignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid signature header");
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = "sha256=" + bytesToHex(digest);

            if (!computedSignature.equals(signature)) {
                log.warn("Signature mismatch! Expected: {}, Received: {}", computedSignature, signature);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying signature", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Signature verification failed");
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
