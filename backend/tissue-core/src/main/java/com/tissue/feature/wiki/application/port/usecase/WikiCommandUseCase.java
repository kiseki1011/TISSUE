package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import org.jspecify.annotations.Nullable;

public interface WikiCommandUseCase {

    DocumentResponse create(DocumentCreateCommand cmd, Long actorMemberId);

    void updateTitle(Long wikiId, String title, Long actorMemberId);

    void updateContent(Long wikiId, UpdateDocumentContentCommand cmd, Long actorMemberId);

    void setParent(Long wikiId, @Nullable Long parentWikiId, Long actorMemberId);

    void addLink(Long wikiId, WikiLinkTargetType targetType, Long targetId, Long actorMemberId);

    void removeLink(Long wikiId, Long wikiLinkId, Long actorMemberId);

    void lock(Long wikiId, Long actorMemberId);

    void unLock(Long wikiId, Long actorMemberId);

    void delete(Long wikiId, Long actorMemberId);

    void restore(Long wikiId, Long actorMemberId);

    void hardDelete(Long wikiId, Long actorMemberId);

    void batchHardDelete(Long actorMemberId);
}
