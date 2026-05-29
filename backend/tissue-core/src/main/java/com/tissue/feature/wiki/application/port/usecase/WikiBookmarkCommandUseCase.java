package com.tissue.feature.wiki.application.port.usecase;

public interface WikiBookmarkCommandUseCase {

    void addBookmark(Long wikiId, Long actorMemberId);

    void removeBookmark(Long wikiId, Long actorMemberId);
}
