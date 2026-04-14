package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WikiAttachmentRepository extends Repository<WikiAttachment, Long> {

    WikiAttachment save(WikiAttachment attachment);

    Optional<WikiAttachment> findByIdAndWorkspaceKey(Long id, String workspaceKey);

    List<WikiAttachment> findByDocumentIdAndWorkspaceKey(Long documentId, String workspaceKey);

    long countByDocumentIdAndWorkspaceKey(Long documentId, String workspaceKey);

    void delete(WikiAttachment attachment);
}
