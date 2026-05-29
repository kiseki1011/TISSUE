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

    public WikiDocument getById(Long wikiDocumentId) {
        return wikiDocumentQueryRepository
                .findById(wikiDocumentId)
                .orElseThrow(() -> new WikiDocumentNotFoundException(wikiDocumentId));
    }

    public WikiDocument getDeletedById(Long wikiDocumentId) {
        return wikiDocumentQueryRepository
                .findDeletedById(wikiDocumentId)
                .orElseThrow(() -> new WikiDocumentNotFoundException(wikiDocumentId));
    }
}
