package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiBookmark;
import com.tissue.feature.wiki.domain.WikiDocument;
import java.time.Instant;

public record WikiBookmarkResponse(
        Long bookmarkId, Long documentId, String title, boolean locked, String currentVersion, Instant bookmarkedAt) {

    public static WikiBookmarkResponse from(WikiBookmark bookmark) {
        WikiDocument document = bookmark.getDocument();
        return new WikiBookmarkResponse(
                bookmark.getId(),
                document.getId(),
                document.getTitle(),
                document.isLocked(),
                document.getCurrentSnapshotVersion().toString(),
                bookmark.getCreatedAt());
    }
}
