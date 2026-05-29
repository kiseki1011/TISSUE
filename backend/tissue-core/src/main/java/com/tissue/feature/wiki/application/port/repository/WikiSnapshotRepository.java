package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WikiSnapshotRepository extends Repository<WikiDocumentSnapshot, Long> {

    WikiDocumentSnapshot save(WikiDocumentSnapshot snapshot);

    Optional<WikiDocumentSnapshot> findById(Long id);

    @Query("""
           SELECT s FROM WikiDocumentSnapshot s
           WHERE s.document.id = :documentId
           ORDER BY s.snapshotVersion.major DESC,
                    s.snapshotVersion.minor DESC,
                    s.snapshotVersion.patch DESC
       """)
    List<WikiDocumentSnapshot> findByDocumentIdOrderByVersionDesc(@Param("documentId") Long documentId);
}
