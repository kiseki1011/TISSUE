package com.tissue.feature.vcs.application.dto;

import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record GitPrDto(
        String projectKey,
        VcsProvider provider,
        PrAction action,
        @Nullable Integer number,
        String title,
        String body,
        String htmlUrl,
        String authorEmail,
        String authorUsername,
        Instant occurredAt,
        boolean merged,
        boolean closed) {}
