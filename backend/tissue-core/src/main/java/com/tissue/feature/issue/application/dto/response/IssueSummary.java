package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record IssueSummary(
        Long id,
        String issueKey,
        String title,
        IssuePriority priority,
        @Nullable Integer storyPoint,
        @Nullable Instant dueAt,
        @Nullable Integer countBasedProgress,
        Long currentStateId,
        String currentStateLabel,
        StateCategory currentStateCategory,
        @Nullable Long assigneeMemberId,
        @Nullable Long sprintId) {

    public static IssueSummary from(Issue issue) {
        WorkflowState state = issue.getCurrentState();
        ProjectMember assignee = issue.getParticipants().getAssignee();
        return new IssueSummary(
                issue.getId(),
                issue.getKey(),
                issue.getTitle(),
                issue.getPriority(),
                issue.getStoryPoint(),
                issue.getSchedule().getDueAt(),
                issue.getProgress().getCountBasedProgress(),
                state.getId(),
                state.getDisplayName(),
                state.getCategory(),
                assignee != null ? assignee.getMemberId() : null,
                issue.getSprint() != null ? issue.getSprint().getId() : null);
    }
}
