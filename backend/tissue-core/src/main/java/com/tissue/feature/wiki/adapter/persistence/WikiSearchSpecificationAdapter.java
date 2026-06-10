package com.tissue.feature.wiki.adapter.persistence;

import com.tissue.feature.wiki.application.port.repository.WikiSearchRepository;
import com.tissue.feature.wiki.domain.WikiDocument;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WikiSearchSpecificationAdapter implements WikiSearchRepository {

    private final WikiDocumentSearchJpaRepository jpaRepository;

    @Override
    public Page<WikiDocument> search(@Nullable String keyword, @Nullable Set<Long> tagIds, Pageable pageable) {
        Specification<WikiDocument> spec = Specification.where(WikiDocumentSearchSpecs.ftsKeywordMatches(keyword))
                .and(WikiDocumentSearchSpecs.hasAnyTags(tagIds))
                .and(WikiDocumentSearchSpecs.orderByRelevance(keyword));

        return jpaRepository.findAll(spec, pageable);
    }
}
