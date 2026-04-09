package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import org.springframework.data.repository.Repository;

public interface WikiSnapshotRepository extends Repository<WikiDocumentSnapshot, Long> {

    WikiDocumentSnapshot save(WikiDocumentSnapshot snapshot);
}
