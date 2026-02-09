package com.tissue.sprint.application.dto.response;

import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.SprintStatus;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record SprintDetail(
        Long id,
        String sprintKey,
        String title,
        @Nullable String goal,
        @Nullable Instant startedAt,
        @Nullable Instant dueAt,
        @Nullable Instant completedAt,
        SprintStatus status,
        Instant createdAt,
        Long createdBy) {

    public static SprintDetail from(Sprint sprint) {
        return SprintDetail.builder()
                .id(sprint.getId())
                .title(sprint.getTitle())
                .goal(sprint.getGoal())
                .startedAt(sprint.getStartedAt())
                .dueAt(sprint.getDueAt())
                .completedAt(sprint.getCompletedAt())
                .status(sprint.getStatus())
                .createdAt(sprint.getCreatedAt())
                .createdBy(sprint.getCreatedBy())
                .build();
    }
}
