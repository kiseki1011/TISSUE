package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiTag;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface WikiTagRepository extends Repository<WikiTag, Long> {

    WikiTag save(WikiTag tag);

    void delete(WikiTag tag);

    Optional<WikiTag> findById(Long id);

    Optional<WikiTag> findByName_NormalizedName(String normalizedName);

    Page<WikiTag> findByName_NormalizedNameContaining(String normalizedName, Pageable pageable);

    Page<WikiTag> findAll(Pageable pageable);
}
