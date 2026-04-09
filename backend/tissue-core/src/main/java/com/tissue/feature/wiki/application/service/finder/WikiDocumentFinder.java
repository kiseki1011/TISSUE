package com.tissue.feature.wiki.application.service.finder;

import com.tissue.feature.wiki.application.port.repository.WikiDocumentRepository;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.exception.WikiDocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiDocumentFinder {

    private final WikiDocumentRepository wikiDocumentRepository;

    public WikiDocument getBy(String workspaceKey, Long wikiDocumentId) {
        return wikiDocumentRepository
                .findByIdAndWorkspaceKey(wikiDocumentId, workspaceKey)
                .orElseThrow(() -> new WikiDocumentNotFoundException(workspaceKey, wikiDocumentId));
    }
}
