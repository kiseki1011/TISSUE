package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiTag;
import lombok.Builder;

@Builder
public record WikiTagDetail(Long tagId, String name, String color) {

    public static WikiTagDetail from(WikiTag tag) {
        return WikiTagDetail.builder()
                .tagId(tag.getId())
                .name(tag.getName())
                .color(tag.getColor().getDisplayName())
                .build();
    }
}
