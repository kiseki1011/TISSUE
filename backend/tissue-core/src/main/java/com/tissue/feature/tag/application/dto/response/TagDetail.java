package com.tissue.feature.tag.application.dto.response;

import com.tissue.feature.tag.domain.Tag;
import lombok.Builder;

@Builder
public record TagDetail(Long tagId, String name, String color, String description) {

    public static TagDetail from(Tag tag) {
        return TagDetail.builder()
                .tagId(tag.getId())
                .name(tag.getName().getDisplayName())
                .color(tag.getColor().name())
                .description(tag.getDescription())
                .build();
    }
}
