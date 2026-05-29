package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocument;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface WikiDocumentCommandRepository extends Repository<WikiDocument, Long> {

    WikiDocument save(WikiDocument document);

    void delete(WikiDocument document);

    @Modifying
    @Query(value = """
            DELETE FROM wiki_document
            WHERE soft_deleted = true
            """, nativeQuery = true)
    void deleteAllSoftDeleted();
}
