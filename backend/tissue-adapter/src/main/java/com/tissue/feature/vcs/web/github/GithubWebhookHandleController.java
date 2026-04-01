package com.tissue.feature.vcs.web.github;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GitHub Integration")
@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class GithubWebhookHandleController {

    private final GithubWebhookService githubWebhookService;

    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    @Operation(summary = "Handle GitHub webhook", description = """
                Receive and process GitHub webhook events. \
                The payload is verified using the `X-Hub-Signature-256` header.""")
    @ApiResponse(responseCode = "200", description = "Webhook processed")
    @PostMapping("/{workspaceKey}/integrations/github/webhook")
    public ResponseEntity<Void> handleGithubWebhook(
            @PathVariable String workspaceKey,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String rawPayload) {
        log.info("Received GitHub webhook for workspace: {}, event type: {}", workspaceKey, eventType);
        githubWebhookService.handleWebhook(workspaceKey, signature, eventType, rawPayload);

        return ResponseEntity.ok().build();
    }
}
