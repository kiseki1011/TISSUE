package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiDocument;
import java.time.Instant;
import lombok.Builder;

@Builder
public record WikiDocumentSummary(
        Long id,
        String title,
        boolean locked,
        String currentVersion,
        boolean hasChildren,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static WikiDocumentSummary from(WikiDocument document, boolean hasChildren) {
        return WikiDocumentSummary.builder()
                .id(document.getId())
                .title(document.getTitle())
                .locked(document.isLocked())
                .currentVersion(document.getCurrentSnapshotVersion().toString())
                .hasChildren(hasChildren)
                .createdAt(document.getCreatedAt())
                .lastModifiedAt(document.getLastModifiedAt())
                .build();
    }
}
