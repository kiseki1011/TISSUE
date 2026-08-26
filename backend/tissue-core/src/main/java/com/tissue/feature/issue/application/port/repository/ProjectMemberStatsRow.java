package com.tissue.feature.issue.application.port.repository;

import org.jspecify.annotations.Nullable;

public interface ProjectMemberStatsRow {

    Long getMemberId();

    long getResolvedCount();

    long getOpenAssignedCount();

    long getTotalStoryPoints();

    @Nullable
    Double getAvgResolveSeconds();
}
