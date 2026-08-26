package com.tissue.feature.vcs.adapter.web.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.feature.vcs.application.dto.VcsEventResult;
import com.tissue.feature.vcs.application.port.usecase.GitProviderUseCase;
import com.tissue.feature.vcs.application.port.usecase.VcsWebhookDispatcher;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Parses stored GitHub payloads and drives the provider-neutral use case.
 *
 * <p>Split out of the receiving service so the inbox can replay a delivery from disk later: a retry has
 * only the raw body to work from, and this is the only place that knows GitHub's JSON shape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GithubWebhookDispatcher implements VcsWebhookDispatcher {

    private final GitProviderUseCase gitProviderUseCase;
    private final ObjectMapper objectMapper;

    private static final String EVENT_PUSH = "push";
    private static final String EVENT_PULL_REQUEST = "pull_request";

    @Override
    public VcsProvider provider() {
        return VcsProvider.GITHUB;
    }

    /**
     * Parse failures come back as skipped rather than thrown: the bytes will not become valid on a second
     * attempt, so retrying them would only burn the retry budget.
     */
    @Override
    public VcsEventResult dispatch(String projectKey, String eventType, String rawPayload) {
        try {
            return switch (eventType) {
                case EVENT_PUSH -> {
                    GithubPushPayload payload = objectMapper.readValue(rawPayload, GithubPushPayload.class);
                    yield gitProviderUseCase.handlePushEvent(payload.toVcsDto(projectKey, VcsProvider.GITHUB));
                }
                case EVENT_PULL_REQUEST -> {
                    GithubPrPayload payload = objectMapper.readValue(rawPayload, GithubPrPayload.class);
                    if (payload.getPullRequest() == null) {
                        yield VcsEventResult.skipped("Pull request event carried no pull_request object");
                    }
                    yield gitProviderUseCase.handlePullRequest(payload.toVcsDto(projectKey, VcsProvider.GITHUB));
                }
                default -> VcsEventResult.skipped("Unhandled GitHub event type: " + eventType);
            };
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse GitHub {} payload for project: {}", eventType, projectKey, e);
            return VcsEventResult.skipped(
                    "Malformed GitHub %s payload: %s".formatted(eventType, e.getOriginalMessage()));
        }
    }
}
