package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.domain.IssuePullRequest;
import com.tissue.feature.issue.domain.enums.PullRequestState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A pull request linked to an issue, surfaced read-only on the issue detail. Populated from inbound
 * pull-request webhooks and kept as current state, so the client can show what is open right now without
 * replaying the issue's activity.
 */
@Schema(description = "A pull request linked to the issue, with its current state.")
public record IssuePullRequestView(
        @Schema(description = "The provider's pull request number, e.g. 42")
        int number,

        @Schema(description = "Pull request title as last seen")
        String title,

        @Schema(description = "Ready-to-open URL of the pull request")
        String url,

        @Schema(description = "Provider username of the author") @Nullable
        String authorName,

        @Schema(description = "Whether the pull request is open, closed, or merged")
        PullRequestState state,

        @Schema(description = "When the last event for this pull request occurred")
        Instant lastEventAt) {

    public static IssuePullRequestView from(IssuePullRequest pullRequest) {
        return new IssuePullRequestView(
                pullRequest.getNumber(),
                pullRequest.getTitle(),
                pullRequest.getUrl(),
                pullRequest.getAuthorName(),
                pullRequest.getState(),
                pullRequest.getLastEventAt());
    }
}
