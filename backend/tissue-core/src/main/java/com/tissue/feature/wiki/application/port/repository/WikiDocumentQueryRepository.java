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

    Optional<WikiDocument> findByIdAndWorkspaceKey(Long id, String workspaceKey);

    /**
     * Finds a soft-deleted document.
     * Uses a native query to bypass {@code @SQLRestriction("soft_deleted = false")}.
     */
    @Query(value = """
            SELECT * FROM wiki_document
            WHERE id = :id
              AND workspace_key = :workspaceKey
              AND soft_deleted = true
            """, nativeQuery = true)
    Optional<WikiDocument> findDeletedByIdAndWorkspaceKey(
            @Param("id") Long id, @Param("workspaceKey") String workspaceKey);

    @Query(value = """
            SELECT * FROM wiki_document
            WHERE workspace_key = :workspaceKey
              AND soft_deleted = true
            """, nativeQuery = true)
    List<WikiDocument> findAllDeletedByWorkspaceKey(@Param("workspaceKey") String workspaceKey);

    @EntityGraph(attributePaths = {"parentDocument"})
    @Query("SELECT d FROM WikiDocument d WHERE d.workspaceKey = :workspaceKey AND d.id = :wikiId")
    Optional<WikiDocument> findWithParentByWorkspaceKeyAndId(
            @Param("workspaceKey") String workspaceKey, @Param("wikiId") Long wikiId);

    @Query("""
           SELECT d FROM WikiDocument d
           WHERE d.workspaceKey = :workspaceKey
             AND d.parentDocument IS NULL
           ORDER BY d.title ASC
       """)
    List<WikiDocument> findRootDocuments(@Param("workspaceKey") String workspaceKey);

    @Query("""
           SELECT d FROM WikiDocument d
           WHERE d.workspaceKey = :workspaceKey
             AND d.parentDocument.id = :parentId
           ORDER BY d.title ASC
       """)
    List<WikiDocument> findChildrenByParentId(
            @Param("workspaceKey") String workspaceKey, @Param("parentId") Long parentId);

    @Query("""
           SELECT d FROM WikiDocument d
           LEFT JOIN FETCH d.parentDocument
           WHERE d.workspaceKey = :workspaceKey
           ORDER BY d.title ASC
       """)
    List<WikiDocument> findAllWithParentByWorkspaceKey(@Param("workspaceKey") String workspaceKey);

    @Query("""
           SELECT d.parentDocument.id
           FROM WikiDocument d
           WHERE d.workspaceKey = :workspaceKey
             AND d.parentDocument IS NOT NULL
           GROUP BY d.parentDocument.id
       """)
    Set<Long> findDocumentIdsWithChildren(@Param("workspaceKey") String workspaceKey);

    @Query("""
           SELECT COUNT(d) > 0
           FROM WikiDocument d
           WHERE d.workspaceKey = :workspaceKey
             AND d.parentDocument.id = :parentId
       """)
    boolean hasChildren(@Param("workspaceKey") String workspaceKey, @Param("parentId") Long parentId);
}
