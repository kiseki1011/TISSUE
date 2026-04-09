package com.tissue.feature.wiki.application.service.finder;

import com.tissue.feature.wiki.application.port.repository.WikiDocumentQueryRepository;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.exception.WikiDocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiDocumentFinder {

    private final WikiDocumentQueryRepository wikiDocumentQueryRepository;

    public WikiDocument getBy(String workspaceKey, Long wikiDocumentId) {
        return wikiDocumentQueryRepository
                .findByIdAndWorkspaceKey(wikiDocumentId, workspaceKey)
                .orElseThrow(() -> new WikiDocumentNotFoundException(workspaceKey, wikiDocumentId));
    }

    public WikiDocument getDeletedBy(String workspaceKey, Long wikiDocumentId) {
        return wikiDocumentQueryRepository
                .findDeletedByIdAndWorkspaceKey(wikiDocumentId, workspaceKey)
                .orElseThrow(() -> new WikiDocumentNotFoundException(workspaceKey, wikiDocumentId));
    }
}
