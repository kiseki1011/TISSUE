package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiTag;
import lombok.Builder;

@Builder
public record WikiTagDetail(Long tagId, String name, String color) {

    public static WikiTagDetail from(WikiTag tag) {
        return WikiTagDetail.builder()
                .tagId(tag.getId())
                .name(tag.getName())
                // Return the enum NAME (e.g. "ANSI_BRIGHT_BLUE", "LIMEGREEN"), not the
                // display name ("ANSI Bright Blue", "Lime Green"). The client renders the
                // colour by feeding this straight to its colour parser (see ColorType's
                // javadoc) and a space-separated display name doesn't parse; the write
                // side (AttachWikiTagRequest) also accepts the enum name, so this keeps
                // read/write consistent.
                .color(tag.getColor().name())
                .build();
    }
}
