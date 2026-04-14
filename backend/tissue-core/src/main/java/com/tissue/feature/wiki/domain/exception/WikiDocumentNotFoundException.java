package com.tissue.feature.wiki.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WIKI_DOCUMENT_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WikiDocumentNotFoundException extends ResourceNotFoundException {

    public WikiDocumentNotFoundException(String workspaceKey, Long wikiDocumentId) {
        super(WikiErrorCode.DOCUMENT_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(WIKI_DOCUMENT_ID, wikiDocumentId);
    }
}
