package com.tissue.feature.wiki.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WIKI_LINK_TARGET_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WIKI_LINK_TARGET_TYPE;

import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WikiLinkTargetNotFoundException extends ResourceNotFoundException {

    public WikiLinkTargetNotFoundException(WikiLinkTargetType targetType, Long targetId) {
        super(WikiErrorCode.LINK_TARGET_NOT_FOUND);
        addContext(WIKI_LINK_TARGET_TYPE, targetType);
        addContext(WIKI_LINK_TARGET_ID, targetId);
    }
}
