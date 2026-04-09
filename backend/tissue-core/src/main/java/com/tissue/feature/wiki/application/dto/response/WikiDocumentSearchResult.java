package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiDocument;
import java.time.Instant;
import lombok.Builder;

@Builder
public record WikiDocumentSearchResult(
        Long id,
        String title,
        String contentSnippet,
        boolean locked,
        String currentVersion,
        Instant createdAt,
        Instant lastModifiedAt) {

    private static final int SNIPPET_LENGTH = 200;

    public static WikiDocumentSearchResult from(WikiDocument document, String keyword) {
        return WikiDocumentSearchResult.builder()
                .id(document.getId())
                .title(document.getTitle())
                .contentSnippet(extractSnippet(document.getContent(), keyword))
                .locked(document.isLocked())
                .currentVersion(document.getCurrentSnapshotVersion().toString())
                .createdAt(document.getCreatedAt())
                .lastModifiedAt(document.getLastModifiedAt())
                .build();
    }

    private static String extractSnippet(String content, String keyword) {
        String lowerContent = content.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int index = lowerContent.indexOf(lowerKeyword);
        if (index == -1) {
            return content.substring(0, Math.min(content.length(), SNIPPET_LENGTH));
        }
        int start = Math.max(0, index - SNIPPET_LENGTH / 2);
        int end = Math.min(content.length(), start + SNIPPET_LENGTH);
        return content.substring(start, end);
    }
}
