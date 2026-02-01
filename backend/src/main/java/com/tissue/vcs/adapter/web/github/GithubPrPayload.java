package com.tissue.vcs.adapter.web.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tissue.vcs.domain.GitPrDto;
import com.tissue.vcs.domain.enums.PrAction;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
@SuppressWarnings("all")
public class GithubPrPayload {

    @Nullable
    private String action = null;

    @Nullable
    @JsonProperty("pull_request")
    private PullRequest pullRequest = null;

    @Nullable
    private Sender sender = null;

    @SuppressWarnings("NullAway.Init")
    protected GithubPrPayload() {
    }

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

        @SuppressWarnings("NullAway.Init")
        protected PullRequest() {
        }
    }

    @Getter
    @Setter
    public static class User {

        @Nullable
        private String login = null;

        @Nullable
        private String email = null;

        @SuppressWarnings("NullAway.Init")
        protected User() {
        }
    }

    @Getter
    @Setter
    public static class Sender {

        @Nullable
        private String login = null;

        @Nullable
        private String email = null;

        @SuppressWarnings("NullAway.Init")
        protected Sender() {
        }
    }

    public GitPrDto toDomainDto(String workspaceKey) {
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
                       .workspaceKey(workspaceKey)
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

        switch (action) {
            case "opened":
                return PrAction.OPENED;
            case "closed":
                return PrAction.CLOSED;
            case "reopened":
                return PrAction.REOPENED;
            case "synchronize":
                return PrAction.UNKNOWN;
            default:
                return PrAction.UNKNOWN;
        }
    }
}
