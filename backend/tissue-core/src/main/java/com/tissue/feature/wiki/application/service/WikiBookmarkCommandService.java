package com.tissue.feature.wiki.application.service;

import com.tissue.feature.wiki.application.port.repository.WikiBookmarkRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkCommandUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiBookmark;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WikiBookmarkCommandService implements WikiBookmarkCommandUseCase {

    private final WikiDocumentFinder wikiDocumentFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WikiBookmarkRepository wikiBookmarkRepository;

    @Override
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
    public void removeBookmark(String workspaceKey, Long wikiId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        wikiBookmarkRepository
                .findByMemberIdAndDocumentIdAndWorkspaceKey(actorMemberId, wikiId, workspaceKey)
                .ifPresent(wikiBookmarkRepository::delete);
    }
}
