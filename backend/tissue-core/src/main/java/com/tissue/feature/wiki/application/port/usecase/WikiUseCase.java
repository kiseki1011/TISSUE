package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.DocumentUpdateCommand;
import com.tissue.feature.wiki.application.dto.request.UploadDocumentAttachmentCommand;
import com.tissue.feature.wiki.application.dto.response.DocResponse;
import org.jspecify.annotations.Nullable;

public interface WikiUseCase {

    DocResponse create(String workspaceKey, DocumentCreateCommand cmd, Long actorMemberId);

    void update(String workspaceKey, Long wikiId, DocumentUpdateCommand cmd, Long actorMemberId);

    void setParent(String workspaceKey, Long wikiId, @Nullable Long parentWikiId, Long actorMemberId);

    void addLink(String workspaceKey, Long wikiId, Long wikiLinkId, Long actorMemberId);

    void removeLink(String workspaceKey, Long wikiId, Long wikiLinkId, Long actorMemberId);

    void lock(String workspaceKey, Long wikiId, Long actorMemberId);

    void unLock(String workspaceKey, Long wikiId, Long actorMemberId);

    void uploadFile(String workspaceKey, Long wikiId, UploadDocumentAttachmentCommand cmd, Long actorMemberId);

    void delete(String workspaceKey, Long wikiId, Long actorMemberId);
}
