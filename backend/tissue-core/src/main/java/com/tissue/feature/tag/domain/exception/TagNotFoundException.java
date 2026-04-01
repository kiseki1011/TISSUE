package com.tissue.feature.tag.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.TAG_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class TagNotFoundException extends ResourceNotFoundException {

    public TagNotFoundException(String projectKey, Long tagId) {
        super(TagErrorCode.TAG_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(TAG_ID, tagId);
    }

    public TagNotFoundException(Long tagId) {
        super(TagErrorCode.TAG_NOT_FOUND);
        addContext(TAG_ID, tagId);
    }
}
