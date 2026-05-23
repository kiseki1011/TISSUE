package com.tissue.feature.issue.persistence;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueTag;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        evaluation = Evaluation.PERFORMANCE_PROBLEM,
        evaluationReason = """
               This implementation is based on the WikiDocumentSearchSpecs I implemented.
               For full-text search performance improvement, using PostgreSQL ts-vector (GIN index)
               should be considered.
               """,
        model = "claude-opus-4-7-max",
        reviewedBy = "kiseki1011")
public final class IssueSearchSpecs {

    private static final String PROJECT = "project";
    private static final String PRIORITY = "priority";
    private static final String TITLE = "title";

    private static final String KEY = "key";
    private static final String KEY_VALUE = "value";

    private static final String CURRENT_STATE = "currentState";
    private static final String STATE_ID = "id";
    private static final String STATE_CATEGORY = "category";

    private static final String PARTICIPANTS = "participants";
    private static final String ASSIGNEE = "assignee";
    private static final String MEMBER_ID = "memberId";

    private static final String SPRINT = "sprint";
    private static final String SPRINT_ID = "id";

    private static final String SCHEDULE = "schedule";
    private static final String DUE_AT = "dueAt";
    private static final String STARTED_AT = "startedAt";
    private static final String RESOLVED_AT = "resolvedAt";

    private static final String PROGRESS = "progress";
    private static final String COUNT_BASED_PROGRESS = "countBasedProgress";

    private static final String TAG_ID = "id";
    private static final String ISSUE = "issue";
    private static final String TAG = "tag";

    private IssueSearchSpecs() {}

    public static Specification<Issue> inProject(Project project) {
        return (root, query, cb) -> cb.equal(root.get(PROJECT), project);
    }

    public static @Nullable Specification<Issue> hasPriorities(@Nullable Set<IssuePriority> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(PRIORITY).in(priorities);
    }

    public static @Nullable Specification<Issue> hasStateCategories(@Nullable Set<StateCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(CURRENT_STATE).get(STATE_CATEGORY).in(categories);
    }

    public static @Nullable Specification<Issue> hasCurrentStateIds(@Nullable Set<Long> stateIds) {
        if (stateIds == null || stateIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(CURRENT_STATE).get(STATE_ID).in(stateIds);
    }

    public static @Nullable Specification<Issue> hasAssignees(@Nullable Set<Long> assigneeMemberIds) {
        if (assigneeMemberIds == null || assigneeMemberIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) ->
                root.get(PARTICIPANTS).get(ASSIGNEE).get(MEMBER_ID).in(assigneeMemberIds);
    }

    public static @Nullable Specification<Issue> inSprints(@Nullable Set<Long> sprintIds) {
        if (sprintIds == null || sprintIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(SPRINT).get(SPRINT_ID).in(sprintIds);
    }

    public static @Nullable Specification<Issue> dueAtBetween(@Nullable Instant from, @Nullable Instant to) {
        return rangeBetween(SCHEDULE, DUE_AT, from, to);
    }

    public static @Nullable Specification<Issue> startedAtBetween(@Nullable Instant from, @Nullable Instant to) {
        return rangeBetween(SCHEDULE, STARTED_AT, from, to);
    }

    public static @Nullable Specification<Issue> resolvedAtBetween(@Nullable Instant from, @Nullable Instant to) {
        return rangeBetween(SCHEDULE, RESOLVED_AT, from, to);
    }

    public static @Nullable Specification<Issue> progressBetween(@Nullable Integer minPct, @Nullable Integer maxPct) {
        if (minPct == null && maxPct == null) {
            return null;
        }
        return (root, query, cb) -> {
            Expression<Integer> progress = root.get(PROGRESS).get(COUNT_BASED_PROGRESS);
            Predicate predicate = cb.conjunction();
            if (minPct != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(progress, minPct));
            }
            if (maxPct != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(progress, maxPct));
            }
            return predicate;
        };
    }

    public static @Nullable Specification<Issue> hasAllTags(@Nullable Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            var tagRoot = subquery.from(IssueTag.class);
            subquery.select(cb.count(tagRoot.get(TAG_ID)))
                    .where(
                            cb.equal(tagRoot.get(ISSUE), root),
                            tagRoot.get(TAG).get(TAG_ID).in(tagIds));
            return cb.equal(subquery, (long) tagIds.size());
        };
    }

    /**
     * TODO:
     * Matches issue key and title against the keyword with case-insensitive LIKE.
     *
     * <p>Content (LOB) is intentionally excluded. Content scans are slow
     * without an index. Full-text search over content is planned via a separate
     * {@code IssueFullTextSearchRepository} using PostgreSQL {@code tsvector} GIN index.
     */
    public static @Nullable Specification<Issue> keywordMatches(@Nullable String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get(KEY).get(KEY_VALUE)), pattern),
                    cb.like(cb.lower(root.get(TITLE)), pattern));
        };
    }

    private static @Nullable Specification<Issue> rangeBetween(
            String embeddedField, String dateField, @Nullable Instant from, @Nullable Instant to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            Expression<Instant> path = root.get(embeddedField).get(dateField);
            Predicate predicate = cb.conjunction();
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(path, from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(path, to));
            }
            return predicate;
        };
    }

    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
