package com.tissue.issue.application.dto.request;

import com.tissue.vcs.domain.enums.VcsProvider;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record PerformSystemTransitionCommand(
        VcsProvider vcsProvider,
        @Nullable String vcsUserEmail,
        @Nullable String vcsUserName,
        @Nullable String triggerReason) {}
