package com.tissue.feature.wiki.persistence;

import com.tissue.feature.wiki.application.port.repository.WikiSearchRepository;
import com.tissue.feature.wiki.domain.WikiDocument;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WikiSearchSpecificationAdapter implements WikiSearchRepository {

    private final WikiDocumentSearchJpaRepository jpaRepository;

    @Override
    public List<WikiDocument> searchByKeyword(
            String keyword, @Nullable Instant keysetModifiedAt, @Nullable Long keysetId, int limit) {
        Specification<WikiDocument> spec = Specification.where(WikiDocumentSearchSpecs.titleOrContentContains(keyword))
                .and(WikiDocumentSearchSpecs.beforeKeyset(keysetModifiedAt, keysetId));

        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by("lastModifiedAt").descending().and(Sort.by("id").descending()));
        return jpaRepository.findAll(spec, pageable).getContent();
    }
}
