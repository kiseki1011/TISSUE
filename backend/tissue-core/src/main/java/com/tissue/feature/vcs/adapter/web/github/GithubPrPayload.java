package com.tissue.feature.vcs.adapter.web.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@SuppressWarnings({"NullAway", "StringConcatToTextBlock"})
public class GithubPrPayload {

    @Nullable
    private String action = null;

    @Nullable
    @JsonProperty("pull_request")
    private PullRequest pullRequest = null;

    @Nullable
    private Sender sender = null;

    protected GithubPrPayload() {}

    @Getter
    @Setter
    public static class PullRequest {

        @Nullable
        private String title = null;

        @Nullable
        private String body = null;

        @Nullable
        @JsonProperty("html_url")
        private String htmlUrl = null;

        private boolean merged;

        @Nullable
        @JsonProperty("created_at")
        private String createdAt = null;

        @Nullable
        private User user = null;

        protected PullRequest() {}
    }

    @Getter
    @Setter
    public static class User {

        @Nullable
        private String login = null;

        @Nullable
        private String email = null;

        protected User() {}
    }

    @Getter
    @Setter
    public static class Sender {

        @Nullable
        private String login = null;

        @Nullable
        private String email = null;

        protected Sender() {}
    }

    public GitPrDto toVcsDto(String projectKey, VcsProvider provider) {
        String title = (pullRequest != null) ? pullRequest.title : null;
        String body = (pullRequest != null) ? pullRequest.body : null;
        String htmlUrl = (pullRequest != null) ? pullRequest.htmlUrl : null;
        boolean merged = (pullRequest != null) && pullRequest.merged;

        String authorUsername = null;
        String authorEmail = null;

        if (pullRequest != null && pullRequest.user != null) {
            authorUsername = pullRequest.user.login;
            authorEmail = pullRequest.user.email;
        }

        return GitPrDto.builder()
                .projectKey(projectKey)
                .provider(provider)
                .action(mapAction())
                .title(title)
                .body(body)
                .htmlUrl(htmlUrl)
                .merged(merged)
                .authorUsername(authorUsername)
                .authorEmail(authorEmail)
                .occurredAt(Instant.now())
                .build();
    }

    private PrAction mapAction() {
        if (action == null) {
            return PrAction.UNKNOWN;
        }
        return switch (action) {
            case "opened" -> PrAction.OPENED;
            case "closed" -> PrAction.CLOSED;
            case "reopened" -> PrAction.REOPENED;
            default -> PrAction.UNKNOWN;
        };
    }
}
