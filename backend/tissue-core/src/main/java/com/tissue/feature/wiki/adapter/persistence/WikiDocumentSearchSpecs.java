package com.tissue.feature.wiki.adapter.persistence;

import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentTag;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.search.FtsQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "This needs review! "
                + "Integration test passes, but performance and edge cases have not been tested. "
                + "If review is done, and code is ACCEPTABLE, WikiSearchSpecificationAdapter / "
                + "WikiQueryService.searchDocuments / WikiSearchRepository evaluation can be changed "
                + "to ACCEPTABLE.",
        model = "claude-opus-4-8")
public final class WikiDocumentSearchSpecs {

    private static final String SEARCH_VECTOR = "searchVector";
    private static final String LAST_MODIFIED_AT = "lastModifiedAt";
    private static final String ID = "id";
    private static final String DOCUMENT = "document";
    private static final String TAG = "tag";

    private WikiDocumentSearchSpecs() {}

    /**
     * tsvector-backed full-text match on the document's {@code search_vector} column
     * (title + content, see {@code tissue-bootstrap/src/main/resources/db/fts.sql}). Reuses the
     * generic {@code fts_match} Hibernate function (shared with issue search); the keyword is turned
     * into a prefix query by {@link FtsQuery#toPrefixQuery} so a partial word ("depl") matches words
     * starting with it ("deployment"). Returns {@code null} for a blank keyword so the filter can be
     * skipped (tag-only / browse).
     */
    public static @Nullable Specification<WikiDocument> ftsKeywordMatches(@Nullable String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.isTrue(cb.function(
                "fts_match", Boolean.class, root.get(SEARCH_VECTOR), cb.literal(FtsQuery.toPrefixQuery(keyword))));
    }

    /**
     * Matches documents tagged with ANY of {@code tagIds} (OR). Correlated EXISTS subquery, so it
     * does not multiply rows. Returns {@code null} when empty.
     */
    public static @Nullable Specification<WikiDocument> hasAnyTags(@Nullable Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<WikiDocumentTag> tagRoot = subquery.from(WikiDocumentTag.class);
            subquery.select(tagRoot.get(ID))
                    .where(
                            cb.equal(tagRoot.get(DOCUMENT), root),
                            tagRoot.get(TAG).get(ID).in(tagIds));
            return cb.exists(subquery);
        };
    }

    /**
     * Sets the relevance ordering: {@code ts_rank} DESC, then {@code lastModifiedAt DESC, id DESC}
     * as tiebreakers. When the keyword is blank (tag-only or unfiltered browse) it falls back to
     * recency. Side-effecting specification (it sets {@code query.orderBy}) and skipped for the
     * count query Spring Data issues under offset pagination. Returns an always-true predicate so
     * it composes with {@link #ftsKeywordMatches} and {@link #hasAnyTags}.
     */
    public static Specification<WikiDocument> orderByRelevance(@Nullable String keyword) {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                if (keyword != null && !keyword.isBlank()) {
                    Expression<Float> rank = cb.function(
                            "fts_rank",
                            Float.class,
                            root.get(SEARCH_VECTOR),
                            cb.literal(FtsQuery.toPrefixQuery(keyword)));
                    query.orderBy(cb.desc(rank), cb.desc(root.get(LAST_MODIFIED_AT)), cb.desc(root.get(ID)));
                } else {
                    query.orderBy(cb.desc(root.get(LAST_MODIFIED_AT)), cb.desc(root.get(ID)));
                }
            }
            return cb.conjunction();
        };
    }
}
