package com.tissue.feature.wiki.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.wiki.application.port.repository.WikiBookmarkRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkCommandUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiBookmark;
import com.tissue.feature.wiki.domain.WikiDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WikiBookmarkCommandService implements WikiBookmarkCommandUseCase {

    private final WikiDocumentFinder wikiDocumentFinder;
    private final MemberFinder memberFinder;
    private final WikiBookmarkRepository wikiBookmarkRepository;

    @Override
    public void addBookmark(Long wikiId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);
        WikiDocument document = wikiDocumentFinder.getById(wikiId);

        boolean alreadyExists = wikiBookmarkRepository.existsByMemberIdAndDocumentId(actorMemberId, wikiId);
        if (alreadyExists) {
            return;
        }

        WikiBookmark bookmark = WikiBookmark.create(actorMemberId, document);
        wikiBookmarkRepository.save(bookmark);
    }

    @Override
    public void removeBookmark(Long wikiId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        wikiBookmarkRepository
                .findByMemberIdAndDocumentId(actorMemberId, wikiId)
                .ifPresent(wikiBookmarkRepository::delete);
    }
}
