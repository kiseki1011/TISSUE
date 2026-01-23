package com.tissue.vcs.domain;

import com.tissue.vcs.domain.enums.PrAction;
import java.time.LocalDateTime;
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
        LocalDateTime occurredAt,
        boolean merged) {}
