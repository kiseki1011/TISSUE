package com.tissue.feature.wiki.adapter.persistence;

import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentTag;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public class WikiDocumentSearchSpecs {

    private static final String TITLE = "title";
    private static final String CONTENT = "content";
    private static final String LAST_MODIFIED_AT = "lastModifiedAt";
    private static final String ID = "id";
    private static final String DOCUMENT = "document";
    private static final String TAG = "tag";

    public static @Nullable Specification<WikiDocument> titleOrContentContains(@Nullable String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            String pattern = "%" + escapeLike(keyword).toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get(TITLE)), pattern), cb.like(cb.lower(root.get(CONTENT)), pattern));
        };
    }

    /**
     * Matches documents tagged with ANY of {@code tagIds} (OR). Correlated EXISTS subquery, so it
     * does not multiply rows — keyset pagination stays correct. Returns {@code null} when empty.
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

    public static @Nullable Specification<WikiDocument> beforeKeyset(
            @Nullable Instant keysetModifiedAt, @Nullable Long keysetId) {
        if (keysetModifiedAt == null || keysetId == null) {
            return null;
        }
        return (root, query, cb) -> cb.or(
                cb.lessThan(root.get(LAST_MODIFIED_AT), keysetModifiedAt),
                cb.and(cb.equal(root.get(LAST_MODIFIED_AT), keysetModifiedAt), cb.lessThan(root.get(ID), keysetId)));
    }

    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
