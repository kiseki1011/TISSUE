package com.tissue.feature.vcs.application.dto;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import lombok.Builder;

@Builder
public record GitPushDto(
        String projectKey,
        VcsProvider provider,
        String ref,
        String repoUrl,
        String pusherName,
        String pusherEmail,
        String latestCommitHash,
        String latestCommitMessage,
        String latestCommitUrl,
        Instant occurredAt) {}
