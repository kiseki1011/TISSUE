package com.tissue.feature.wiki.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WIKI_DOCUMENT_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WikiDocumentNotFoundException extends ResourceNotFoundException {

    public WikiDocumentNotFoundException(Long wikiDocumentId) {
        super(WikiErrorCode.DOCUMENT_NOT_FOUND);
        addContext(WIKI_DOCUMENT_ID, wikiDocumentId);
    }
}
