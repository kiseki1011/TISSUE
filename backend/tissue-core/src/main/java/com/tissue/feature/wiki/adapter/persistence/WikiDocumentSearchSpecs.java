package com.tissue.feature.wiki.adapter.persistence;

import com.tissue.feature.wiki.domain.WikiDocument;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public class WikiDocumentSearchSpecs {

    private static final String TITLE = "title";
    private static final String CONTENT = "content";
    private static final String LAST_MODIFIED_AT = "lastModifiedAt";
    private static final String ID = "id";

    public static Specification<WikiDocument> titleOrContentContains(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + escapeLike(keyword).toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get(TITLE)), pattern), cb.like(cb.lower(root.get(CONTENT)), pattern));
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
