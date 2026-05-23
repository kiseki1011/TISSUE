package com.tissue.feature.sprint.application.dto.response;

import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record SprintSummary(
        Long id,
        String sprintKey,
        String title,
        String goal,
        SprintStatus status,
        @Nullable Instant startedAt,
        @Nullable Instant dueAt,
        @Nullable Instant completedAt) {

    public static SprintSummary from(Sprint sprint) {
        return new SprintSummary(
                sprint.getId(),
                sprint.getSprintKey(),
                sprint.getTitle(),
                sprint.getGoal(),
                sprint.getStatus(),
                sprint.getStartedAt(),
                sprint.getDueAt(),
                sprint.getCompletedAt());
    }
}
