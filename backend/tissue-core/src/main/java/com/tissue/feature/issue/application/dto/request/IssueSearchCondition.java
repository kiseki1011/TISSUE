package com.tissue.feature.issue.application.dto.request;

import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record IssueSearchCondition(
        @Nullable Set<IssuePriority> priorities,
        @Nullable Set<StateCategory> stateCategories,
        @Nullable Set<Long> currentStateIds,
        @Nullable Set<Long> issueTypeIds,
        @Nullable Set<Long> tagIds,
        @Nullable Set<Long> authorMemberIds,
        @Nullable Set<Long> assigneeMemberIds,
        @Nullable Set<Long> reviewerMemberIds,
        @Nullable Set<ReviewStatus> reviewerStatuses,
        @Nullable Set<Long> subscriberMemberIds,
        @Nullable Set<Long> sprintIds,
        @Nullable Boolean currentSprintOnly,
        @Nullable Instant dueAtFrom,
        @Nullable Instant dueAtTo,
        @Nullable String keyword) {

    public static IssueSearchCondition empty() {
        return new IssueSearchCondition(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Whether any non-keyword filter is set.
     *
     * <p>Lets a keyword-less search still run as a pure filter query (example: "issues assigned to me")
     * instead of returning an empty page.
     */
    public boolean hasActiveFilters() {
        return notEmpty(priorities)
                || notEmpty(stateCategories)
                || notEmpty(currentStateIds)
                || notEmpty(issueTypeIds)
                || notEmpty(tagIds)
                || notEmpty(authorMemberIds)
                || notEmpty(assigneeMemberIds)
                || notEmpty(reviewerMemberIds)
                || notEmpty(subscriberMemberIds)
                || notEmpty(sprintIds)
                || Boolean.TRUE.equals(currentSprintOnly)
                || dueAtFrom != null
                || dueAtTo != null;
    }

    private static boolean notEmpty(@Nullable Set<?> set) {
        return set != null && !set.isEmpty();
    }
}
