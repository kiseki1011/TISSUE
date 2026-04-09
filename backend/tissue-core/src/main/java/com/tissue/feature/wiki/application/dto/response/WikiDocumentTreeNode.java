package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiDocument;
import org.jspecify.annotations.Nullable;

public record WikiDocumentTreeNode(
        Long id, String title, boolean locked, @Nullable Long parentDocumentId) {

    public static WikiDocumentTreeNode from(WikiDocument document) {
        return new WikiDocumentTreeNode(
                document.getId(),
                document.getTitle(),
                document.isLocked(),
                document.getParentDocument() != null
                        ? document.getParentDocument().getId()
                        : null);
    }
}
