package com.tissue.feature.vcs.application.dto;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record GitPushDto(
        String workspaceKey,
        VcsProvider provider,
        String ref,
        String repoUrl,
        String pusherName,
        String pusherEmail,
        String latestCommitHash,
        String latestCommitMessage,
        String latestCommitUrl,
        LocalDateTime occurredAt) {}
