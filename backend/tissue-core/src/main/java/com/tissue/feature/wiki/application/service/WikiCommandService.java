package com.tissue.feature.wiki.application.service;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.DOCUMENT_HAS_CHILDREN;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.LINK_NOT_FOUND;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentCommandRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentQueryRepository;
import com.tissue.feature.wiki.application.port.repository.WikiLinkRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiCommandUseCase;
import com.tissue.feature.wiki.application.service.authorization.WikiAuthorizationService;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.WikiLink;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WikiCommandService implements WikiCommandUseCase {

    private final WikiDocumentCommandRepository wikiDocumentCommandRepository;
    private final WikiSnapshotRepository wikiSnapshotRepository;
    private final WikiLinkRepository wikiLinkRepository;
    private final WikiDocumentQueryRepository wikiDocumentQueryRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WikiDocumentFinder wikiDocumentFinder;
    private final WikiLinkTargetResolver wikiLinkTargetResolver;
    private final WikiAuthorizationService wikiAuthorizationService;

    @Override
    public DocumentResponse create(String workspaceKey, DocumentCreateCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        Workspace workspace = actor.getWorkspace();

        WikiDocument parentDocument = resolveParentDocument(workspaceKey, cmd.parentDocumentId());

        WikiDocument document = WikiDocument.create(workspace, cmd.title(), cmd.content(), parentDocument);
        wikiDocumentCommandRepository.save(document);

        return DocumentResponse.from(document);
    }

    @Override
    public void updateTitle(String workspaceKey, Long wikiId, String title, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        document.updateTitle(title);
    }

    @Override
    public void updateContent(String workspaceKey, Long wikiId, UpdateDocumentContentCommand cmd, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        document.updateContent(cmd.content(), cmd.versionUpdateType());

        WikiDocumentSnapshot snapshot =
                WikiDocumentSnapshot.create(document, cmd.versionUpdateType(), cmd.editReason());
        wikiSnapshotRepository.save(snapshot);
    }

    @Override
    public void setParent(String workspaceKey, Long wikiId, @Nullable Long parentWikiId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiDocument parentDocument = resolveParentDocument(workspaceKey, parentWikiId);
        document.setParent(parentDocument);
    }

    @Override
    public void addLink(
            String workspaceKey, Long wikiId, WikiLinkTargetType targetType, Long targetId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);

        String targetWorkspaceKey = wikiLinkTargetResolver.resolveWorkspaceKey(targetType, targetId);

        WikiLink link = WikiLink.create(document, targetType, targetId, targetWorkspaceKey);
        wikiLinkRepository.save(link);
    }

    @Override
    public void removeLink(String workspaceKey, Long wikiId, Long wikiLinkId, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiLink link = wikiLinkRepository
                .findByWorkspaceKeyAndId(workspaceKey, wikiLinkId)
                .orElseThrow(() -> new ResourceNotFoundException(LINK_NOT_FOUND));

        wikiLinkRepository.delete(link);
    }

    @Override
    public void lock(String workspaceKey, Long wikiId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        wikiAuthorizationService.requireDocumentLockPermission(document, actor);

        document.lock();
    }

    @Override
    public void unLock(String workspaceKey, Long wikiId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        wikiAuthorizationService.requireDocumentLockPermission(document, actor);

        document.unLock();
    }

    @Override
    public void delete(String workspaceKey, Long wikiId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);
        wikiAuthorizationService.requireDocumentDeletePermission(document, actor);

        document.softDelete();
    }

    @Override
    public void restore(String workspaceKey, Long wikiId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WikiDocument document = wikiDocumentFinder.getDeletedBy(workspaceKey, wikiId);
        wikiAuthorizationService.requireDocumentDeletePermission(document, actor);

        document.restoreSoftDeleted();
    }

    @Override
    public void hardDelete(String workspaceKey, Long wikiId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WikiDocument document = wikiDocumentFinder.getDeletedBy(workspaceKey, wikiId);
        wikiAuthorizationService.requireDocumentDeletePermission(document, actor);
        ensureNoChildren(workspaceKey, wikiId);

        wikiDocumentCommandRepository.delete(document);
    }

    @Override
    public void batchHardDelete(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        List<WikiDocument> deletedDocuments = wikiDocumentQueryRepository.findAllDeletedByWorkspaceKey(workspaceKey);
        for (WikiDocument document : deletedDocuments) {
            wikiAuthorizationService.requireDocumentDeletePermission(document, actor);
        }

        wikiDocumentCommandRepository.deleteAllSoftDeletedByWorkspaceKey(workspaceKey);
    }

    private void ensureNoChildren(String workspaceKey, Long wikiId) {
        if (wikiDocumentQueryRepository.hasChildren(workspaceKey, wikiId)) {
            throw new BadRequestException(DOCUMENT_HAS_CHILDREN);
        }
    }

    @Nullable
    private WikiDocument resolveParentDocument(String workspaceKey, @Nullable Long parentDocumentId) {
        if (parentDocumentId == null) {
            return null;
        }
        return wikiDocumentFinder.getBy(workspaceKey, parentDocumentId);
    }
}
