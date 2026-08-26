package com.tissue.feature.vcs.adapter.web.github;

import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.application.service.VcsWebhookInboxService;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.ProjectVcsIntegrationNotFoundException;
import com.tissue.feature.vcs.domain.support.WebhookSignatureVerifier;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Authenticates an inbound GitHub webhook and hands it to the inbox. Does no parsing and no domain work,
 * so the request returns as soon as the delivery is safely on disk.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubWebhookService {

    private final ProjectVcsIntegrationRepository vcsIntegrationRepository;
    private final WebhookSignatureVerifier signatureVerifier;
    private final VcsWebhookInboxService inboxService;

    private static final String UNKNOWN_EVENT_TYPE = "unknown";

    public void handleWebhook(
            String projectKey,
            @Nullable String signature,
            @Nullable String deliveryId,
            @Nullable String eventType,
            String rawPayload) {

        ProjectVcsIntegration integration = vcsIntegrationRepository
                .findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB)
                .orElseThrow(
                        () -> new ProjectVcsIntegrationNotFoundException(projectKey, VcsProvider.GITHUB.toString()));

        signatureVerifier.verifySignature(rawPayload, signature, integration.getWebhookSecret());

        inboxService.receive(
                VcsProvider.GITHUB,
                resolveDeliveryId(deliveryId, projectKey),
                projectKey,
                eventType != null ? eventType : UNKNOWN_EVENT_TYPE,
                rawPayload);
    }

    /**
     * GitHub always stamps a delivery id, but a caller that omits it must not be able to collapse every
     * delivery onto one row, so an absent id gets a unique substitute and simply forfeits deduplication.
     */
    private String resolveDeliveryId(@Nullable String deliveryId, String projectKey) {
        if (deliveryId != null && !deliveryId.isBlank()) {
            return deliveryId;
        }

        log.warn("GitHub webhook for project {} carried no delivery id; deduplication is not possible", projectKey);
        return "no-delivery-id-" + UUID.randomUUID();
    }
}
