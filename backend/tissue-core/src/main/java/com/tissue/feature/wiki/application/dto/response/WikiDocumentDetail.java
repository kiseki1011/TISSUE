package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiLink;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record WikiDocumentDetail(
        Long id,
        String title,
        String content,
        boolean locked,
        String currentVersion,
        @Nullable Long parentDocumentId,
        @Nullable String parentDocumentTitle,
        List<WikiLinkInfo> links,
        Long createdBy,
        Long lastModifiedBy,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static WikiDocumentDetail from(WikiDocument document, List<WikiLink> links) {
        return WikiDocumentDetail.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .locked(document.isLocked())
                .currentVersion(document.getCurrentSnapshotVersion().toString())
                .parentDocumentId(
                        document.getParentDocument() != null
                                ? document.getParentDocument().getId()
                                : null)
                .parentDocumentTitle(
                        document.getParentDocument() != null
                                ? document.getParentDocument().getTitle()
                                : null)
                .links(links.stream().map(WikiLinkInfo::from).toList())
                .createdBy(document.getCreatedBy())
                .lastModifiedBy(document.getLastModifiedBy())
                .createdAt(document.getCreatedAt())
                .lastModifiedAt(document.getLastModifiedAt())
                .build();
    }
}
