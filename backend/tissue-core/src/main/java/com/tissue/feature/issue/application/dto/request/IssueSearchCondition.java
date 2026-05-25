package com.tissue.feature.issue.application.dto.request;

import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record IssueSearchCondition(
        @Nullable Set<IssuePriority> priorities,
        @Nullable Set<StateCategory> stateCategories,
        @Nullable Set<Long> currentStateIds,
        @Nullable Set<Long> tagIds,
        @Nullable Set<Long> authorMemberIds,
        @Nullable Set<Long> assigneeMemberIds,
        @Nullable Set<Long> reviewerMemberIds,
        @Nullable Set<Long> subscriberMemberIds,
        @Nullable Set<Long> sprintIds,
        @Nullable Boolean currentSprintOnly,
        @Nullable Instant dueAtFrom,
        @Nullable Instant dueAtTo,
        @Nullable String keyword) {

    public static IssueSearchCondition empty() {
        return new IssueSearchCondition(null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
