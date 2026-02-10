package com.tissue.feature.vcs.application.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record GitPushDto(
        String workspaceKey,
        String ref,
        String repoUrl,
        String pusherName,
        String pusherEmail,
        String latestCommitHash,
        String latestCommitMessage,
        String latestCommitUrl,
        LocalDateTime occurredAt) {}
