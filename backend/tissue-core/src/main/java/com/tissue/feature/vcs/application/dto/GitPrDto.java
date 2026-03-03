package com.tissue.feature.vcs.application.dto;

import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import lombok.Builder;

@Builder
public record GitPrDto(
        String workspaceKey,
        VcsProvider provider,
        PrAction action,
        String title,
        String body,
        String htmlUrl,
        String authorEmail,
        String authorUsername,
        Instant occurredAt,
        boolean merged) {}
