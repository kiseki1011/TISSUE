package com.tissue.feature.wiki.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WIKI_DOCUMENT_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WIKI_SNAPSHOT_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WikiSnapshotNotFoundException extends ResourceNotFoundException {

    public WikiSnapshotNotFoundException(Long wikiDocumentId, Long snapshotId) {
        super(WikiErrorCode.SNAPSHOT_NOT_FOUND);
        addContext(WIKI_DOCUMENT_ID, wikiDocumentId);
        addContext(WIKI_SNAPSHOT_ID, snapshotId);
    }
}
