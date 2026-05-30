package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocument;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface WikiSearchRepository {

    List<WikiDocument> searchByKeyword(
            String keyword, @Nullable Instant keysetModifiedAt, @Nullable Long keysetId, int limit);
}
