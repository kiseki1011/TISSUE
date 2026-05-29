package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocument;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WikiDocumentQueryRepository extends Repository<WikiDocument, Long> {

    Optional<WikiDocument> findById(Long id);

    /**
     * Finds a soft-deleted document.
     * Uses a native query to bypass {@code @SQLRestriction("soft_deleted = false")}.
     */
    @Query(value = """
            SELECT * FROM wiki_document
            WHERE id = :id
              AND soft_deleted = true
            """, nativeQuery = true)
    Optional<WikiDocument> findDeletedById(@Param("id") Long id);

    @Query(value = """
            SELECT * FROM wiki_document
            WHERE soft_deleted = true
            """, nativeQuery = true)
    List<WikiDocument> findAllDeleted();

    @EntityGraph(attributePaths = {"parentDocument"})
    @Query("SELECT d FROM WikiDocument d WHERE d.id = :wikiId")
    Optional<WikiDocument> findWithParentById(@Param("wikiId") Long wikiId);

    @Query("""
           SELECT d FROM WikiDocument d
           WHERE d.parentDocument IS NULL
           ORDER BY d.title ASC
       """)
    List<WikiDocument> findRootDocuments();

    @Query("""
           SELECT d FROM WikiDocument d
           WHERE d.parentDocument.id = :parentId
           ORDER BY d.title ASC
       """)
    List<WikiDocument> findChildrenByParentId(@Param("parentId") Long parentId);

    @Query("""
           SELECT d FROM WikiDocument d
           LEFT JOIN FETCH d.parentDocument
           ORDER BY d.title ASC
       """)
    List<WikiDocument> findAllWithParent();

    @Query("""
           SELECT d.parentDocument.id
           FROM WikiDocument d
           WHERE d.parentDocument IS NOT NULL
           GROUP BY d.parentDocument.id
       """)
    Set<Long> findDocumentIdsWithChildren();

    @Query("""
           SELECT COUNT(d) > 0
           FROM WikiDocument d
           WHERE d.parentDocument.id = :parentId
       """)
    boolean hasChildren(@Param("parentId") Long parentId);
}
