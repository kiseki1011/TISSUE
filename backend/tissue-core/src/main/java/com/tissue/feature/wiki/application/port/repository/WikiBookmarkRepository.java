package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiBookmark;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WikiBookmarkRepository extends Repository<WikiBookmark, Long> {

    WikiBookmark save(WikiBookmark bookmark);

    void delete(WikiBookmark bookmark);

    Optional<WikiBookmark> findByMemberIdAndDocumentId(Long memberId, Long documentId);

    boolean existsByMemberIdAndDocumentId(Long memberId, Long documentId);

    @EntityGraph(attributePaths = {"document"})
    @Query("""
           SELECT b FROM WikiBookmark b
           WHERE b.memberId = :memberId
           ORDER BY b.createdAt DESC
       """)
    List<WikiBookmark> findAllWithDocumentByMemberId(@Param("memberId") Long memberId);
}
