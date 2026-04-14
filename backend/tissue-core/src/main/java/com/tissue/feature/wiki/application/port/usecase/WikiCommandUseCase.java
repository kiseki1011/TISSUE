package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import org.jspecify.annotations.Nullable;

public interface WikiCommandUseCase {

    DocumentResponse create(String workspaceKey, DocumentCreateCommand cmd, Long actorMemberId);

    void updateTitle(String workspaceKey, Long wikiId, String title, Long actorMemberId);

    void updateContent(String workspaceKey, Long wikiId, UpdateDocumentContentCommand cmd, Long actorMemberId);

    void setParent(String workspaceKey, Long wikiId, @Nullable Long parentWikiId, Long actorMemberId);

    void addLink(String workspaceKey, Long wikiId, WikiLinkTargetType targetType, Long targetId, Long actorMemberId);

    void removeLink(String workspaceKey, Long wikiId, Long wikiLinkId, Long actorMemberId);

    void lock(String workspaceKey, Long wikiId, Long actorMemberId);

    void unLock(String workspaceKey, Long wikiId, Long actorMemberId);

    void delete(String workspaceKey, Long wikiId, Long actorMemberId);

    void restore(String workspaceKey, Long wikiId, Long actorMemberId);

    void hardDelete(String workspaceKey, Long wikiId, Long actorMemberId);

    void batchHardDelete(String workspaceKey, Long actorMemberId);
}
