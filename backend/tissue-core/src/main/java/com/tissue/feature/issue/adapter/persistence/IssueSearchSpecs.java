package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.IssueSubscriber;
import com.tissue.feature.issue.domain.IssueTag;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.search.FtsQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "This needs thorough review. After review, if code is acceptable, "
                + "all related code including IssueFullTextAdapter, IssueFullTextSearchRepository, "
                + "IssueFullTextSearchService evaluation can be changed to ACCEPTABLE.",
        model = "claude-opus-4-7 + claude-opus-4-8")
public final class IssueSearchSpecs {

    private static final String PROJECT = "project";
    private static final String PRIORITY = "priority";
    private static final String SEARCH_VECTOR = "searchVector";

    private static final String CURRENT_STATE = "currentState";
    private static final String STATE_ID = "id";
    private static final String STATE_CATEGORY = "category";

    private static final String PARTICIPANTS = "participants";
    private static final String ASSIGNEE = "assignee";
    private static final String MEMBER = "member";
    private static final String MEMBER_PK = "id";
    private static final String REVIEWER = "reviewer";
    private static final String REVIEW_STATUS = "status";
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

    /**
     * Restricts to issues whose project id is in {@code projectIds}, used by the instance-wide search
     * to scope results to the caller's project memberships ({@link ProjectMember}).
     */
    public static Specification<Issue> inProjectIds(Set<Long> projectIds) {
        return (root, query, cb) -> root.get(PROJECT).get("id").in(projectIds);
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
                root.get(PARTICIPANTS).get(ASSIGNEE).get(MEMBER).get(MEMBER_PK).in(assigneeMemberIds);
    }

    public static @Nullable Specification<Issue> hasReviewers(
            @Nullable Set<Long> reviewerMemberIds, @Nullable Set<ReviewStatus> reviewerStatuses) {
        if (reviewerMemberIds == null || reviewerMemberIds.isEmpty()) {
            return null;
        }
        boolean byStatus = reviewerStatuses != null && !reviewerStatuses.isEmpty();
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            var reviewerRoot = subquery.from(IssueReviewer.class);
            Predicate match = cb.and(
                    cb.equal(reviewerRoot.get(ISSUE), root),
                    reviewerRoot.get(REVIEWER).get(MEMBER).get(MEMBER_PK).in(reviewerMemberIds));
            // Optionally require the matching reviewer's status to be one of the requested ones
            if (byStatus) {
                match = cb.and(match, reviewerRoot.get(REVIEW_STATUS).in(reviewerStatuses));
            }
            subquery.select(cb.literal(1L)).where(match);
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
                            subscriberRoot
                                    .get(SUBSCRIBER)
                                    .get(MEMBER)
                                    .get(MEMBER_PK)
                                    .in(subscriberMemberIds));
            return cb.exists(subquery);
        };
    }

    public static @Nullable Specification<Issue> inSprints(@Nullable Set<Long> sprintIds) {
        if (sprintIds == null || sprintIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(SPRINT).get(SPRINT_ID).in(sprintIds);
    }

    public static Specification<Issue> noSprint() {
        return (root, query, cb) -> cb.isNull(root.get(SPRINT));
    }

    public static @Nullable Specification<Issue> dueAtBetween(@Nullable Instant from, @Nullable Instant to) {
        return rangeBetween(SCHEDULE, DUE_AT, from, to);
    }

    /**
     * Keyset cursor predicate matching rows that come strictly AFTER the given (priority, id) tuple
     * under the fixed sort {@code priority ASC, id DESC}.
     *
     * <p>SQL form: {@code (priority > :p) OR (priority = :p AND id < :id)}.
     * Returns {@code null} when {@code cursor} is null so the first page is unconstrained.
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
     * Matches the issue's author ({@code created_by} column).
     * Can be used by the "issues I created" / "issues created by member X" use cases.
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
     * tsvector-backed full-text match on the issue's {@code search_vector} column
     * (issue_key + title + content, see {@code tissue-bootstrap/src/main/resources/db/fts.sql}).
     *
     * <p>Builds {@code fts_match(issue.search_vector, :query)} via the {@link IssueFtsFunctionContributor}
     * -registered pattern function, which expands to {@code (search_vector @@ to_tsquery('simple', :query))}
     * and uses the GIN index. The keyword is turned into a prefix query by {@link FtsQuery#toPrefixQuery}
     * so a partial word ("depl") matches words that start with it ("deployment"). Used by the
     * {@code searchProjectIssues} endpoint, composable with all other {@link IssueSearchSpecs} filters.
     */
    public static @Nullable Specification<Issue> ftsKeywordMatches(@Nullable String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.isTrue(cb.function(
                "fts_match", Boolean.class, root.get(SEARCH_VECTOR), cb.literal(FtsQuery.toPrefixQuery(keyword))));
    }

    /**
     * Sets the relevance ordering for full-text search: {@code ts_rank} of the keyword against {@code search_vector}
     * DESC, then {@code priority ASC, id DESC} as deterministic tiebreakers.
     *
     * <p>Implemented as a side-effecting specification (it sets {@code query.orderBy}) and skipped
     * for the count query Spring Data issues under offset pagination. Returns an always-true predicate
     * so it composes with {@link #ftsKeywordMatches} and the other filters.
     */
    public static Specification<Issue> orderByRelevance(@Nullable String keyword) {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                if (keyword != null && !keyword.isBlank()) {
                    Expression<Float> rank = cb.function(
                            "fts_rank",
                            Float.class,
                            root.get(SEARCH_VECTOR),
                            cb.literal(FtsQuery.toPrefixQuery(keyword)));
                    query.orderBy(cb.desc(rank), cb.asc(root.get(PRIORITY)), cb.desc(root.get("id")));
                } else {
                    query.orderBy(cb.asc(root.get(PRIORITY)), cb.desc(root.get("id")));
                }
            }
            return cb.conjunction();
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
}
