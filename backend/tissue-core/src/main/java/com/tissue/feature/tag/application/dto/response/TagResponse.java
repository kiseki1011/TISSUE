package com.tissue.feature.tag.application.dto.response;

import com.tissue.feature.tag.domain.Tag;

public record TagResponse(Long tagId) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId());
    }
}
