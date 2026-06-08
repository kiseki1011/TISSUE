package com.tissue.feature.wiki.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WIKI_TAG_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WikiTagNotFoundException extends ResourceNotFoundException {

    public WikiTagNotFoundException(Long wikiTagId) {
        super(WikiErrorCode.TAG_NOT_FOUND);
        addContext(WIKI_TAG_ID, wikiTagId);
    }
}
