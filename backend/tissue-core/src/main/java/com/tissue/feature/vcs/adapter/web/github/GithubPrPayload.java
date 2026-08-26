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
        private Integer number = null;

        @Nullable
        private String state = null;

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
        Integer number = (pullRequest != null) ? pullRequest.number : null;
        boolean merged = isMerged();
        boolean closed = (pullRequest != null) && "closed".equals(pullRequest.state);

        String authorUsername = null;
        String authorEmail = null;

        // TODO: carry `sender` through as the actor, separately from the author below.
        //  GitHub sends two people: `pull_request.user` is who OPENED the pull request and never changes,
        //  while `sender` is who caused THIS event - who merged, closed or reopened it. They differ for
        //  every action but "opened". Only the author is read here, so VcsIntegrationService matches the
        //  project member by the author's email and then credits them with the merge: the activity entry
        //  and, worse, the automatic workflow transition both name the person who wrote the branch rather
        //  than the person who merged it.
        //  Fix: add actorUsername/actorEmail to GitPrDto from `sender`, leave author* as it is (
        //  IssuePullRequest.authorName genuinely wants the author), and have
        //  VcsIntegrationService.handlePullRequest resolve its ProjectMember from actorEmail.
        //  Note this does not improve matching - GitHub omits `sender.email` as often as `user.email`, so
        //  most events will still fall through to performTransitionBySystem. It changes WHO is named when
        //  a match does happen, which today is always the wrong person for a merge.
        //  ActivitySentence.pullRequest (tissue-mcp) words the MCP activity feed passively to avoid
        //  asserting this; it can go back to the active voice once this is fixed.
        if (pullRequest != null && pullRequest.user != null) {
            authorUsername = pullRequest.user.login;
            authorEmail = pullRequest.user.email;
        }

        return GitPrDto.builder()
                .projectKey(projectKey)
                .provider(provider)
                .action(mapAction())
                .number(number)
                .title(title)
                .body(body)
                .htmlUrl(htmlUrl)
                .merged(merged)
                .closed(closed)
                .authorUsername(authorUsername)
                .authorEmail(authorEmail)
                .occurredAt(Instant.now())
                .build();
    }

    /**
     * GitHub reports a merge as {@code action=closed} with {@code pull_request.merged=true}; the flag is the
     * only thing separating it from a plain close, so a merge is invisible without checking it.
     */
    private PrAction mapAction() {
        if (action == null) {
            return PrAction.UNKNOWN;
        }
        return switch (action) {
            case "opened" -> PrAction.OPENED;
            case "closed" -> isMerged() ? PrAction.MERGED : PrAction.CLOSED;
            case "reopened" -> PrAction.REOPENED;
            default -> PrAction.UNKNOWN;
        };
    }

    private boolean isMerged() {
        return pullRequest != null && pullRequest.merged;
    }
}
