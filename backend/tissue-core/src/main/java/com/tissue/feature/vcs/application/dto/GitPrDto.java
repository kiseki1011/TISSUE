package com.tissue.feature.vcs.application.dto;

import com.tissue.feature.vcs.domain.enums.PrAction;
import java.time.Instant;
import lombok.Builder;

@Builder
public record GitPrDto(
        String workspaceKey,
        PrAction action,
        String title,
        String body,
        String htmlUrl,
        String authorEmail,
        String authorUsername,
        Instant occurredAt,
        boolean merged) {}
