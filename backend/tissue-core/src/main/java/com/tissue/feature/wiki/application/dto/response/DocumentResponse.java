package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiDocument;

public record DocumentResponse(Long id, String title) {

    public static DocumentResponse from(WikiDocument document) {
        return new DocumentResponse(document.getId(), document.getTitle());
    }
}
