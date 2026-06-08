package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocumentTag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface WikiDocumentTagRepository extends Repository<WikiDocumentTag, Long> {

    /**
     * Purges join rows for soft-deleted documents. Must run BEFORE the native bulk
     * {@code deleteAllSoftDeleted()} on wiki_document, which bypasses JPA cascade and would
     * otherwise hit an FK violation on these rows.
     */
    @Modifying
    @Query(value = """
            DELETE FROM wiki_document_tag
            WHERE wiki_document_id IN (SELECT id FROM wiki_document WHERE soft_deleted = true)
            """, nativeQuery = true)
    void deleteAllBySoftDeletedDocuments();
}
