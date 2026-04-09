package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WikiSnapshotRepository extends Repository<WikiDocumentSnapshot, Long> {

    WikiDocumentSnapshot save(WikiDocumentSnapshot snapshot);

    Optional<WikiDocumentSnapshot> findByIdAndWorkspaceKey(Long id, String workspaceKey);

    @Query("""
           SELECT s FROM WikiDocumentSnapshot s
           WHERE s.document.id = :documentId
             AND s.workspaceKey = :workspaceKey
           ORDER BY s.snapshotVersion.major DESC,
                    s.snapshotVersion.minor DESC,
                    s.snapshotVersion.patch DESC
       """)
    List<WikiDocumentSnapshot> findByDocumentIdOrderByVersionDesc(
            @Param("documentId") Long documentId, @Param("workspaceKey") String workspaceKey);
}
