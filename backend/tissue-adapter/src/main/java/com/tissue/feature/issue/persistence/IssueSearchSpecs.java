package com.tissue.feature.issue.persistence;

import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.IssueSubscriber;
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
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.PERFORMANCE_PROBLEM,
        evaluationReason = "Works, but has horrible performance.",
        model = "claude-opus-4-7-max")
public final class IssueSearchSpecs {

    private static final String PROJECT = "project";
    private static final String PRIORITY = "priority";
    private static final String TITLE = "title";
    private static final String SEARCH_VECTOR = "searchVector";

    private static final String KEY = "key";
    private static final String KEY_VALUE = "value";

    private static final String CURRENT_STATE = "currentState";
    private static final String STATE_ID = "id";
    private static final String STATE_CATEGORY = "category";

    private static final String PARTICIPANTS = "participants";
    private static final String ASSIGNEE = "assignee";
    private static final String MEMBER_ID = "memberId";
    private static final String REVIEWER = "reviewer";
    private static final String SUBSCRIBER = "subscriber";
    private static final String SPRINT = "sprint";
    private static final String SPRINT_ID = "id";

    private static final String SCHEDULE = "schedule";
    private static final String DUE_AT = "dueAt";

    private static final String TAG_ID = "id";
    private static final String ISSUE = "issue";
    private static final String TAG = "tag";

    private static final String CREATED_BY = "createdBy";

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

    public static @Nullable Specification<Issue> hasReviewers(@Nullable Set<Long> reviewerMemberIds) {
        if (reviewerMemberIds == null || reviewerMemberIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            var reviewerRoot = subquery.from(IssueReviewer.class);
            subquery.select(cb.literal(1L))
                    .where(
                            cb.equal(reviewerRoot.get(ISSUE), root),
                            reviewerRoot.get(REVIEWER).get(MEMBER_ID).in(reviewerMemberIds));
            return cb.exists(subquery);
        };
    }

    public static @Nullable Specification<Issue> hasSubscribers(@Nullable Set<Long> subscriberMemberIds) {
        if (subscriberMemberIds == null || subscriberMemberIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            var subscriberRoot = subquery.from(IssueSubscriber.class);
            subquery.select(cb.literal(1L))
                    .where(
                            cb.equal(subscriberRoot.get(ISSUE), root),
                            subscriberRoot.get(SUBSCRIBER).get(MEMBER_ID).in(subscriberMemberIds));
            return cb.exists(subquery);
        };
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

    /**
     * Keyset cursor predicate matching rows that come strictly AFTER the
     * given (priority, id) tuple under the fixed sort {@code priority ASC, id DESC}.
     *
     * <p>SQL form: {@code (priority > :p) OR (priority = :p AND id < :id)}.
     * Returns {@code null} when {@code cursor} is null so the first page is
     * unconstrained.
     */
    public static @Nullable Specification<Issue> afterCursor(@Nullable IssueSearchCursor cursor) {
        if (cursor == null) {
            return null;
        }
        return (root, query, cb) -> cb.or(
                cb.greaterThan(root.get(PRIORITY), cursor.priority()),
                cb.and(cb.equal(root.get(PRIORITY), cursor.priority()), cb.lessThan(root.get("id"), cursor.id())));
    }

    /**
     * Matches the issue's author (audit {@code created_by} column).
     * Used by the "issues I created" / "issues created by member X" use cases.
     */
    public static @Nullable Specification<Issue> hasAuthors(@Nullable Set<Long> authorMemberIds) {
        if (authorMemberIds == null || authorMemberIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(CREATED_BY).in(authorMemberIds);
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
     * Matches issue key and title against the keyword with case-insensitive LIKE.
     * Used by the LIKE-based {@code searchProjectIssues} endpoint.
     *
     * <p>Content is excluded — without an index, content scans are slow at scale.
     * For content-aware search use {@link #ftsKeywordMatches} on the FTS endpoint.
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

    /**
     * tsvector-backed full-text match on the issue's {@code search_vector} column
     * (issue_key + title + content, see {@code loadtest/seed/fts.sql}).
     *
     * <p>Builds {@code fts_match(issue.search_vector, :keyword)} via the
     * {@link IssueFtsFunctionContributor}-registered pattern function, which expands
     * to {@code (search_vector @@ plainto_tsquery('simple', :keyword))} and uses the
     * GIN index. Used by the {@code ftsProjectIssues} endpoint, composable with all
     * other {@link IssueSearchSpecs} filters.
     */
    public static @Nullable Specification<Issue> ftsKeywordMatches(@Nullable String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) ->
                cb.isTrue(cb.function("fts_match", Boolean.class, root.get(SEARCH_VECTOR), cb.literal(keyword)));
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
