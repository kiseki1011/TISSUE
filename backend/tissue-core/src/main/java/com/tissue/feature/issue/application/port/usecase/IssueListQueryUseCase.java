package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.ProjectIdentifier;
import org.jspecify.annotations.Nullable;

public interface IssueListQueryUseCase {

    /**
     * Issues (non-terminal) assigned to the actor within the project.
     */
    CursorPage<IssueSummary> getMyWork(ProjectIdentifier pid, Long actorMemberId, @Nullable String cursor, int size);

    /**
     * Issues that are unscheduled (no sprint), not started (INITIAL) in the project.
     */
    CursorPage<IssueSummary> getBacklog(ProjectIdentifier pid, Long actorMemberId, @Nullable String cursor, int size);

    /**
     * Issues in the project's active sprint. Empty when the project has no active sprint.
     */
    CursorPage<IssueSummary> getCurrentSprintIssues(
            ProjectIdentifier pid, Long actorMemberId, @Nullable String cursor, int size);
}
