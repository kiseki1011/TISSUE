package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
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
        @Nullable Long sprintId,
        @Nullable Long issueTypeId,
        @Nullable String issueTypeName,
        @Nullable ColorType issueTypeColor,

        // The actor's own review status on this issue, or null when they are not a reviewer.
        // Only the FTS search service (which knows the actor) populates it. Other leave it null.
        @Nullable ReviewStatus myReviewStatus,
        @Nullable String content) {

    public static IssueSummary from(Issue issue) {
        return from(issue, null);
    }

    public static IssueSummary from(Issue issue, @Nullable ReviewStatus myReviewStatus) {
        return create(issue, myReviewStatus, null);
    }

    public static IssueSummary fromDeleted(Issue issue) {
        return create(issue, null, issue.getContent());
    }

    private static IssueSummary create(Issue issue, @Nullable ReviewStatus myReviewStatus, @Nullable String content) {
        WorkflowState state = issue.getCurrentState();
        ProjectMember assignee = issue.getParticipants().getAssignee();
        IssueType issueType = issue.getIssueType();
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
                issue.getSprint() != null ? issue.getSprint().getId() : null,
                issueType != null ? issueType.getId() : null,
                issueType != null ? issueType.getName() : null,
                issueType != null ? issueType.getColor() : null,
                myReviewStatus,
                content);
    }
}
