package com.tissue.feature.wiki.application.service;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.LINK_NOT_FOUND;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentRepository;
import com.tissue.feature.wiki.application.port.repository.WikiLinkRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.WikiLink;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.exception.base.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WikiCommandService implements WikiUseCase {

    private final WikiDocumentRepository wikiDocumentRepository;
    private final WikiSnapshotRepository wikiSnapshotRepository;
    private final WikiLinkRepository wikiLinkRepository;
    private final WorkspaceFinder workspaceFinder;
    private final WikiDocumentFinder wikiDocumentFinder;
    private final WikiLinkTargetResolver wikiLinkTargetResolver;

    @Override
    public DocumentResponse create(String workspaceKey, DocumentCreateCommand cmd, Long actorMemberId) {
        Workspace workspace = workspaceFinder.getBy(workspaceKey);

        WikiDocument parentDocument = resolveParentDocument(workspaceKey, cmd.parentDocumentId());

        WikiDocument document = WikiDocument.create(workspace, cmd.title(), cmd.content(), parentDocument);
        wikiDocumentRepository.save(document);

        return DocumentResponse.from(document);
    }

    @Override
    public void updateTitle(String workspaceKey, Long wikiId, String title, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        document.updateTitle(title);
    }

    @Override
    public void updateContent(String workspaceKey, Long wikiId, UpdateDocumentContentCommand cmd, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        document.updateContent(cmd.content(), cmd.versionUpdateType());

        WikiDocumentSnapshot snapshot =
                WikiDocumentSnapshot.create(document, cmd.versionUpdateType(), cmd.editReason());
        wikiSnapshotRepository.save(snapshot);
    }

    @Override
    public void setParent(String workspaceKey, Long wikiId, @Nullable Long parentWikiId, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiDocument parentDocument = resolveParentDocument(workspaceKey, parentWikiId);
        document.setParent(parentDocument);
    }

    @Override
    public void addLink(
            String workspaceKey, Long wikiId, WikiLinkTargetType targetType, Long targetId, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);

        String targetWorkspaceKey = wikiLinkTargetResolver.resolveWorkspaceKey(targetType, targetId);

        WikiLink link = WikiLink.create(document, targetType, targetId, targetWorkspaceKey);
        wikiLinkRepository.save(link);
    }

    @Override
    public void removeLink(String workspaceKey, Long wikiId, Long wikiLinkId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiLink link = wikiLinkRepository
                .findByWorkspaceKeyAndId(workspaceKey, wikiLinkId)
                .orElseThrow(() -> new BadRequestException(LINK_NOT_FOUND));

        wikiLinkRepository.delete(link);
    }

    @Override
    public void lock(String workspaceKey, Long wikiId, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        document.lock();
    }

    @Override
    public void unLock(String workspaceKey, Long wikiId, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        document.unLock();
    }

    @Override
    public void delete(String workspaceKey, Long wikiId, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        document.softDelete();
    }

    @Nullable
    private WikiDocument resolveParentDocument(String workspaceKey, @Nullable Long parentDocumentId) {
        if (parentDocumentId == null) {
            return null;
        }
        return wikiDocumentFinder.getBy(workspaceKey, parentDocumentId);
    }
}
