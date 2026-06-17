package com.tissue.feature.issue.adapter.web.request;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Identical shape to {@link IssueSearchCondition} except that the memberId sets accept the string
 * {@code "me"}, which {@link #toCondition(Long)} swaps for the current member id before building
 * the application layer condition.
 *
 * <p>The {@code "me"} sentinel lets clients say "issues authored by me" or "issues assigned to me"
 * without having to know their own numeric member id.
 *
 * <p>example:
 * <pre>
 * {@code ?assigneeMemberIds=me}
 * {@code ?assigneeMemberIds=me,42}
 * </pre>
 */
public record IssueSearchRequest(
        @Nullable Set<IssuePriority> priorities,
        @Nullable Set<StateCategory> stateCategories,
        @Nullable Set<Long> currentStateIds,
        @Nullable Set<Long> tagIds,
        @Nullable Set<String> authorMemberIds,
        @Nullable Set<String> assigneeMemberIds,
        @Nullable Set<String> reviewerMemberIds,
        @Nullable Set<String> subscriberMemberIds,
        @Nullable Set<Long> sprintIds,
        @Nullable Boolean currentSprintOnly,
        @Nullable Instant dueAtFrom,
        @Nullable Instant dueAtTo,
        @Nullable String keyword) {

    private static final String ME = "me";

    public IssueSearchCondition toCondition(Long currentMemberId) {
        return new IssueSearchCondition(
                priorities,
                stateCategories,
                currentStateIds,
                tagIds,
                resolveMe(authorMemberIds, currentMemberId),
                resolveMe(assigneeMemberIds, currentMemberId),
                resolveMe(reviewerMemberIds, currentMemberId),
                resolveMe(subscriberMemberIds, currentMemberId),
                sprintIds,
                currentSprintOnly,
                dueAtFrom,
                dueAtTo,
                keyword);
    }

    /**
     * Replaces the literal {@code "me"} token with {@code currentMemberId} and
     * parses the remaining values as numeric member ids. Returns {@code null}
     * for empty input.
     */
    private static @Nullable Set<Long> resolveMe(@Nullable Set<String> raw, Long currentMemberId) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Set<Long> resolved = new LinkedHashSet<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (ME.equalsIgnoreCase(value.trim())) {
                resolved.add(currentMemberId);
            } else {
                resolved.add(Long.valueOf(value.trim()));
            }
        }
        return resolved.isEmpty() ? null : resolved;
    }
}
