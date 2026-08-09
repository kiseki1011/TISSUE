package com.tissue.feature.vcs.adapter.web.github;

import com.tissue.feature.vcs.domain.exception.VcsErrorCode;
import com.tissue.global.openapi.VcsErrors;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class GithubWebhookHandleController {

    private final GithubWebhookService githubWebhookService;

    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String DELIVERY_HEADER = "X-GitHub-Delivery";
    private static final String EVENT_HEADER = "X-GitHub-Event";

    /**
     * Answers as soon as the delivery is stored, before it is processed. Anything past signature
     * verification is retried from the inbox, so a caller learns only whether the delivery was accepted.
     */
    @Hidden
    @VcsErrors({
        VcsErrorCode.INTEGRATION_NOT_FOUND,
        VcsErrorCode.MISSING_SIGNATURE,
        VcsErrorCode.INVALID_WEBHOOK_SECRET,
    })
    @PostMapping("/{projectKey}/integrations/github/webhook")
    public ResponseEntity<Void> handleGithubWebhook(
            @PathVariable String projectKey,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) @Nullable String signature,
            @RequestHeader(value = DELIVERY_HEADER, required = false) @Nullable String deliveryId,
            @RequestHeader(value = EVENT_HEADER, required = false) @Nullable String eventType,
            @RequestBody String rawPayload) {
        log.info(
                "Received GitHub webhook for project: {}, event type: {}, delivery: {}",
                projectKey,
                eventType,
                deliveryId);
        githubWebhookService.handleWebhook(projectKey, signature, deliveryId, eventType, rawPayload);

        return ResponseEntity.accepted().build();
    }
}
