package com.tissue.feature.wiki.application.service;

import com.tissue.feature.wiki.application.dto.response.WikiBookmarkResponse;
import com.tissue.feature.wiki.application.port.repository.WikiBookmarkRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiBookmark;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WikiBookmarkService implements WikiBookmarkUseCase {

    private final WikiDocumentFinder wikiDocumentFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WikiBookmarkRepository wikiBookmarkRepository;

    @Override
    @Transactional
    public void addBookmark(String workspaceKey, Long wikiId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);

        boolean alreadyExists = wikiBookmarkRepository.existsByMemberIdAndDocumentIdAndWorkspaceKey(
                actorMemberId, wikiId, workspaceKey);
        if (alreadyExists) {
            return;
        }

        WikiBookmark bookmark = WikiBookmark.create(actorMemberId, document);
        wikiBookmarkRepository.save(bookmark);
    }

    @Override
    @Transactional
    public void removeBookmark(String workspaceKey, Long wikiId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        wikiBookmarkRepository
                .findByMemberIdAndDocumentIdAndWorkspaceKey(actorMemberId, wikiId, workspaceKey)
                .ifPresent(wikiBookmarkRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WikiBookmarkResponse> getBookmarks(String workspaceKey, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        List<WikiBookmark> bookmarks =
                wikiBookmarkRepository.findAllWithDocumentByMemberIdAndWorkspaceKey(actorMemberId, workspaceKey);

        return bookmarks.stream().map(WikiBookmarkResponse::from).toList();
    }
}
