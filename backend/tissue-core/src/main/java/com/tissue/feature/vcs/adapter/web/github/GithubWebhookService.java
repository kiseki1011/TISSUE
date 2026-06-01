package com.tissue.feature.vcs.adapter.web.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.GitProviderUseCase;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.ProjectVcsIntegrationNotFoundException;
import com.tissue.feature.vcs.domain.support.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubWebhookService {

    private final GitProviderUseCase gitProviderUseCase;
    private final ProjectVcsIntegrationRepository vcsIntegrationRepository;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    private static final String EVENT_PUSH = "push";
    private static final String EVENT_PULL_REQUEST = "pull_request";

    public void handleWebhook(String projectKey, String signature, String eventType, String rawPayload) {
        log.info("Processing GitHub webhook for project: {}, event: {}", projectKey, eventType);

        ProjectVcsIntegration integration = vcsIntegrationRepository
                .findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB)
                .orElseThrow(
                        () -> new ProjectVcsIntegrationNotFoundException(projectKey, VcsProvider.GITHUB.toString()));

        signatureVerifier.verifySignature(rawPayload, signature, integration.getWebhookSecret());

        try {
            switch (eventType) {
                case EVENT_PUSH -> {
                    GithubPushPayload payload = objectMapper.readValue(rawPayload, GithubPushPayload.class);
                    gitProviderUseCase.handlePushEvent(payload.toVcsDto(projectKey, VcsProvider.GITHUB));
                }
                case EVENT_PULL_REQUEST -> {
                    GithubPrPayload payload = objectMapper.readValue(rawPayload, GithubPrPayload.class);
                    if (payload.getPullRequest() != null) {
                        gitProviderUseCase.handlePullRequest(payload.toVcsDto(projectKey, VcsProvider.GITHUB));
                    }
                }
                default -> log.debug("Ignored GitHub event type: {}", eventType);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse GitHub {} payload for project: {}", eventType, projectKey, e);
        } catch (Exception e) {
            log.error("Error processing GitHub {} webhook for project: {}", eventType, projectKey, e);
        }
    }
}
