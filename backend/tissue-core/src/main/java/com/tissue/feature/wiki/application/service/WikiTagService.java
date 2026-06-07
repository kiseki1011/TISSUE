package com.tissue.feature.wiki.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.wiki.application.dto.request.AttachWikiTagCommand;
import com.tissue.feature.wiki.application.dto.response.WikiTagResponse;
import com.tissue.feature.wiki.application.port.repository.WikiTagRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiTagUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.application.service.finder.WikiTagFinder;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiTag;
import com.tissue.shared.enums.ColorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WikiTagService implements WikiTagUseCase {

    private final MemberFinder memberFinder;
    private final WikiDocumentFinder wikiDocumentFinder;
    private final WikiTagFinder wikiTagFinder;
    private final WikiTagRepository wikiTagRepository;

    @Override
    public WikiTagResponse attachTag(Long wikiId, AttachWikiTagCommand cmd, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getById(wikiId);
        WikiTag tag = getOrCreateTag(cmd);

        document.addTag(tag);

        return WikiTagResponse.from(tag);
    }

    @Override
    public void detachTag(Long wikiId, Long tagId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getById(wikiId);
        WikiTag tag = wikiTagFinder.getById(tagId);

        document.removeTag(tag);
    }

    private WikiTag getOrCreateTag(AttachWikiTagCommand cmd) {
        return wikiTagRepository
                .findByName_NormalizedName(cmd.name().getNormalizedName())
                .orElseGet(() -> {
                    ColorType color = cmd.color() != null ? cmd.color() : ColorType.getRandomColor();
                    return wikiTagRepository.save(WikiTag.create(cmd.name(), cmd.description(), color));
                });
    }
}
