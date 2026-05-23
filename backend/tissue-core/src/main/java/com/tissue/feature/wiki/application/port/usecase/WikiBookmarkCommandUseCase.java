package com.tissue.feature.wiki.application.port.usecase;

public interface WikiBookmarkCommandUseCase {

    void addBookmark(String workspaceKey, Long wikiId, Long actorMemberId);

    void removeBookmark(String workspaceKey, Long wikiId, Long actorMemberId);
}
