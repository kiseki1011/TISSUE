package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocument;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public interface WikiSearchRepository {

    List<WikiDocument> search(
            @Nullable String keyword,
            @Nullable Set<Long> tagIds,
            @Nullable Instant keysetModifiedAt,
            @Nullable Long keysetId,
            int limit);
}
