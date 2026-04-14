package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WikiLinkRepository extends Repository<WikiLink, Long> {

    WikiLink save(WikiLink link);

    Optional<WikiLink> findByWorkspaceKeyAndId(String workspaceKey, Long id);

    List<WikiLink> findBySourceDocumentIdAndWorkspaceKey(Long sourceDocumentId, String workspaceKey);

    void delete(WikiLink link);
}
