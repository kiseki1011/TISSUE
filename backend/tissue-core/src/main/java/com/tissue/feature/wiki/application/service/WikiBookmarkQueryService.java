package com.tissue.feature.wiki.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.wiki.application.dto.response.WikiBookmarkResponse;
import com.tissue.feature.wiki.application.port.repository.WikiBookmarkRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkQueryUseCase;
import com.tissue.feature.wiki.domain.WikiBookmark;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WikiBookmarkQueryService implements WikiBookmarkQueryUseCase {

    private final MemberFinder memberFinder;
    private final WikiBookmarkRepository wikiBookmarkRepository;

    @Override
    public List<WikiBookmarkResponse> getBookmarks(Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        List<WikiBookmark> bookmarks = wikiBookmarkRepository.findAllWithDocumentByMemberId(actorMemberId);

        return bookmarks.stream().map(WikiBookmarkResponse::from).toList();
    }
}
