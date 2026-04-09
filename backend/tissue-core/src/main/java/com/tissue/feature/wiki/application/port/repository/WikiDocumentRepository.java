package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocument;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WikiDocumentRepository extends Repository<WikiDocument, Long> {

    WikiDocument save(WikiDocument document);

    Optional<WikiDocument> findById(Long id);

    Optional<WikiDocument> findByIdAndWorkspaceKey(Long id, String workspaceKey);

    void delete(WikiDocument document);
}
