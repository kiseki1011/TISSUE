package com.tissue.feature.wiki.application.service;

import com.tissue.feature.wiki.application.dto.response.WikiBookmarkResponse;
import com.tissue.feature.wiki.application.port.repository.WikiBookmarkRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkQueryUseCase;
import com.tissue.feature.wiki.domain.WikiBookmark;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WikiBookmarkQueryService implements WikiBookmarkQueryUseCase {

    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WikiBookmarkRepository wikiBookmarkRepository;

    @Override
    public List<WikiBookmarkResponse> getBookmarks(String workspaceKey, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        List<WikiBookmark> bookmarks =
                wikiBookmarkRepository.findAllWithDocumentByMemberIdAndWorkspaceKey(actorMemberId, workspaceKey);

        return bookmarks.stream().map(WikiBookmarkResponse::from).toList();
    }
}
