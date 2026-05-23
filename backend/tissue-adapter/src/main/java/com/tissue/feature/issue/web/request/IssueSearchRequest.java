package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record IssueSearchRequest(
        @Nullable Set<IssuePriority> priorities,
        @Nullable Set<StateCategory> stateCategories,
        @Nullable Set<Long> currentStateIds,
        @Nullable Set<Long> tagIds,
        @Nullable Set<Long> assigneeMemberIds,
        @Nullable Set<Long> reviewerMemberIds,
        @Nullable Set<Long> sprintIds,
        @Nullable Boolean currentSprintOnly,
        @Nullable Instant dueAtFrom,
        @Nullable Instant dueAtTo,
        @Nullable Instant startedAtFrom,
        @Nullable Instant startedAtTo,
        @Nullable Instant resolvedAtFrom,
        @Nullable Instant resolvedAtTo,
        @Nullable Integer progressMinPercent,
        @Nullable Integer progressMaxPercent,
        @Nullable String keyword) {

    public IssueSearchCondition toCondition() {
        return new IssueSearchCondition(
                priorities,
                stateCategories,
                currentStateIds,
                tagIds,
                assigneeMemberIds,
                reviewerMemberIds,
                sprintIds,
                currentSprintOnly,
                dueAtFrom,
                dueAtTo,
                startedAtFrom,
                startedAtTo,
                resolvedAtFrom,
                resolvedAtTo,
                progressMinPercent,
                progressMaxPercent,
                keyword);
    }
}
