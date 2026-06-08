package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public interface IssueListQueryRepository {

    List<Issue> findAssignedAfter(
            Project project,
            Set<Long> memberIds,
            Set<StateCategory> categories,
            @Nullable IssueSearchCursor cursor,
            int limit);

    List<Issue> findBacklogAfter(
            Project project, Set<StateCategory> categories, @Nullable IssueSearchCursor cursor, int limit);

    List<Issue> findInSprintAfter(Project project, Long sprintId, @Nullable IssueSearchCursor cursor, int limit);
}
