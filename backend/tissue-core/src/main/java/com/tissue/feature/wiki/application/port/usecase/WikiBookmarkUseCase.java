package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.response.WikiBookmarkResponse;
import java.util.List;

public interface WikiBookmarkUseCase {

    void addBookmark(String workspaceKey, Long wikiId, Long actorMemberId);

    void removeBookmark(String workspaceKey, Long wikiId, Long actorMemberId);

    List<WikiBookmarkResponse> getBookmarks(String workspaceKey, Long actorMemberId);
}
