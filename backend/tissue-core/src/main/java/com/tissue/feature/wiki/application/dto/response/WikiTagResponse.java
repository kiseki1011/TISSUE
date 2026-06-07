package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiTag;

public record WikiTagResponse(Long tagId) {

    public static WikiTagResponse from(WikiTag tag) {
        return new WikiTagResponse(tag.getId());
    }
}
