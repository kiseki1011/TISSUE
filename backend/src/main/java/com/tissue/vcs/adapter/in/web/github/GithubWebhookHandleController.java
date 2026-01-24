package com.tissue.vcs.adapter.in.web.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.global.exception.base.InternalServerException;
import com.tissue.vcs.application.port.in.GitProviderUseCase;
import com.tissue.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.vcs.domain.enums.VcsProvider;
import com.tissue.vcs.domain.exception.VcsErrorCode;
import com.tissue.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class GithubWebhookHandleController {

    private final GitProviderUseCase gitProviderUseCase;
    private final WorkspaceVcsIntegrationRepository vcsIntegrationRepository;
    private final ObjectMapper objectMapper;

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    @PostMapping("/{workspaceKey}/integrations/github/webhook")
    public ResponseEntity<Void> handleGithubWebhook(
            @PathVariable String workspaceKey,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String rawPayload) {

        log.info("[VCS_WEBHOOK] Received GitHub webhook for workspace: {}, event: {}", workspaceKey, eventType);

        WorkspaceVcsIntegration integration = vcsIntegrationRepository
                .findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(workspaceKey));

        verifySignature(rawPayload, signature, integration.getWebhookSecret());

        try {
            if ("push".equals(eventType)) {
                GithubPushPayload payload = objectMapper.readValue(rawPayload, GithubPushPayload.class);
                gitProviderUseCase.handlePushEvent(payload.toDomainDto(workspaceKey));
            } else if ("pull_request".equals(eventType)) {
                GithubPrPayload payload = objectMapper.readValue(rawPayload, GithubPrPayload.class);
                if (payload.getPullRequest() != null) {
                    gitProviderUseCase.handlePullRequest(payload.toDomainDto(workspaceKey));
                }
            } else {
                log.debug("Ignored event type: {}", eventType);
            }
        } catch (JsonProcessingException e) {
            log.error("[VCS_WEBHOOK_ERROR] Failed to parse GitHub payload", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Invalid JSON payload";
            throw new BadRequestException(VcsErrorCode.INVALID_WEBHOOK_PAYLOAD, errorMsg);
        } catch (Exception e) {
            log.error("[VCS_WEBHOOK_ERROR] Unexpected error processing GitHub webhook", e);
            throw new InternalServerException(VcsErrorCode.WEBHOOK_PROCESSING_ERROR, e);
        }

        return ResponseEntity.ok().build();
    }

    private void verifySignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            throw new BadRequestException(VcsErrorCode.MISSING_SIGNATURE);
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = "sha256=" + bytesToHex(digest);

            if (!computedSignature.equals(signature)) {
                log.warn("[VCS_WEBHOOK_ERROR] Signature mismatch! Expected: {}, Received: {}", computedSignature, signature);
                throw new ForbiddenException(VcsErrorCode.INVALID_WEBHOOK_SECRET);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[VCS_WEBHOOK_ERROR] Error verifying signature", e);
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
