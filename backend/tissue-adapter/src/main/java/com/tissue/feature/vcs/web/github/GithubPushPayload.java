package com.tissue.feature.vcs.web.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Data
@Slf4j
@SuppressWarnings({"NullAway", "StringConcatToTextBlock"})
public class GithubPushPayload {

    @Nullable
    private String ref = null;

    @Nullable
    private Repository repository = null;

    @Nullable
    private Pusher pusher = null;

    @Nullable
    @JsonProperty("head_commit")
    private HeadCommit headCommit = null;

    protected GithubPushPayload() {}

    @Getter
    @Setter
    public static class Repository {

        @Nullable
        @JsonProperty("html_url")
        private String htmlUrl = null;

        protected Repository() {}
    }

    @Getter
    @Setter
    public static class Pusher {

        @Nullable
        private String name = null;

        @Nullable
        private String email = null;

        protected Pusher() {}
    }

    @Getter
    @Setter
    public static class HeadCommit {

        @Nullable
        private String id = null;

        @Nullable
        private String message = null;

        @Nullable
        private String url = null;

        @Nullable
        private String timestamp = null;

        protected HeadCommit() {}
    }

    public GitPushDto toVcsDto(String workspaceKey, VcsProvider provider) {
        String repoUrl = repository != null ? repository.htmlUrl : null;
        String pusherName = pusher != null ? pusher.name : null;
        String pusherEmail = pusher != null ? pusher.email : null;
        String commitHash = headCommit != null ? headCommit.id : null;
        String commitMsg = headCommit != null ? headCommit.message : null;
        String commitUrl = headCommit != null ? headCommit.url : null;

        Instant occurredAt = Instant.now();
        if (headCommit != null && headCommit.timestamp != null) {
            try {
                occurredAt = ZonedDateTime.parse(headCommit.timestamp).toInstant();
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse GitHub commit timestamp: {}", headCommit.timestamp);
            }
        }

        return GitPushDto.builder()
                .workspaceKey(workspaceKey)
                .provider(provider)
                .ref(ref)
                .repoUrl(repoUrl)
                .pusherName(pusherName)
                .pusherEmail(pusherEmail)
                .latestCommitHash(commitHash)
                .latestCommitMessage(commitMsg)
                .latestCommitUrl(commitUrl)
                .occurredAt(occurredAt)
                .build();
    }
}
