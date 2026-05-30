package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WikiAttachmentRepository extends Repository<WikiAttachment, Long> {

    WikiAttachment save(WikiAttachment attachment);

    Optional<WikiAttachment> findById(Long id);

    List<WikiAttachment> findByDocumentId(Long documentId);

    long countByDocumentId(Long documentId);

    void delete(WikiAttachment attachment);
}
