package com.tissue.feature.wiki.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WIKI_ATTACHMENT_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WIKI_DOCUMENT_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WikiAttachmentNotFoundException extends ResourceNotFoundException {

    public WikiAttachmentNotFoundException(Long wikiDocumentId, Long attachmentId) {
        super(WikiErrorCode.ATTACHMENT_NOT_FOUND);
        addContext(WIKI_DOCUMENT_ID, wikiDocumentId);
        addContext(WIKI_ATTACHMENT_ID, attachmentId);
    }
}
