package com.tissue.feature.issue.application.port.repository;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Per-sprint velocity aggregate over a project's COMPLETED sprints: how many of the sprint's issues are
 * currently in a completed state and their summed story points. Story points exclude EPIC issues, whose
 * points are a rollup of their STANDARD children and would double-count when both sit in the same sprint.
 *
 * <p>Attribution reflects an issue's <em>current</em> sprint membership and current state, not the scope
 * as it stood when the sprint closed - the same live-snapshot limitation the sprint report carries. A
 * completed sprint that never held any issue does not appear (it has no velocity signal).
 */
public interface VelocitySprintRow {

    Long getSprintId();

    Long getSprintNumber();

    String getTitle();

    @Nullable
    Instant getCompletedAt();

    long getCompletedIssues();

    long getCompletedStoryPoints();
}
