package com.tissue.feature.issue.application.dto.request;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record PerformSystemTransitionCommand(
        VcsProvider vcsProvider,
        @Nullable String vcsUserEmail,
        @Nullable String vcsUserName,
        @Nullable String triggerReason) {}
