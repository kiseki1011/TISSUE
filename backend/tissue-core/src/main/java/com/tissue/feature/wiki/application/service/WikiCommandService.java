package com.tissue.feature.wiki.application.service;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.DOCUMENT_HAS_CHILDREN;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.LINK_NOT_FOUND;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentCommandRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentQueryRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentTagRepository;
import com.tissue.feature.wiki.application.port.repository.WikiLinkRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiCommandUseCase;
import com.tissue.feature.wiki.application.service.authorization.WikiAuthorizationService;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.WikiLink;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
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
    private final WikiDocumentTagRepository wikiDocumentTagRepository;
    private final MemberFinder memberFinder;
    private final WikiDocumentFinder wikiDocumentFinder;
    private final WikiLinkTargetResolver wikiLinkTargetResolver;
    private final WikiAuthorizationService wikiAuthorizationService;

    @Override
    public DocumentResponse create(DocumentCreateCommand cmd, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        WikiDocument parentDocument = resolveParentDocument(cmd.parentDocumentId());

        WikiDocument document = WikiDocument.create(cmd.title(), cmd.content(), parentDocument);
        wikiDocumentCommandRepository.save(document);

        return DocumentResponse.from(document);
    }

    @Override
    public void updateTitle(Long wikiId, String title, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getById(wikiId);
        document.updateTitle(title);
    }

    @Override
    public void updateContent(Long wikiId, UpdateDocumentContentCommand cmd, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getById(wikiId);
        document.updateContent(cmd.content(), cmd.versionUpdateType());

        WikiDocumentSnapshot snapshot =
                WikiDocumentSnapshot.create(document, cmd.versionUpdateType(), cmd.editReason());
        wikiSnapshotRepository.save(snapshot);
    }

    @Override
    public void setParent(Long wikiId, @Nullable Long parentWikiId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getById(wikiId);

        WikiDocument parentDocument = resolveParentDocument(parentWikiId);
        document.setParent(parentDocument);
    }

    @Override
    public void addLink(Long wikiId, WikiLinkTargetType targetType, Long targetId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getById(wikiId);

        wikiLinkTargetResolver.ensureTargetExists(targetType, targetId);

        WikiLink link = WikiLink.create(document, targetType, targetId);
        wikiLinkRepository.save(link);
    }

    @Override
    public void removeLink(Long wikiId, Long wikiLinkId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        wikiDocumentFinder.getById(wikiId);

        WikiLink link = wikiLinkRepository
                .findById(wikiLinkId)
                .orElseThrow(() -> new ResourceNotFoundException(LINK_NOT_FOUND));

        wikiLinkRepository.delete(link);
    }

    @Override
    public void lock(Long wikiId, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);
        WikiDocument document = wikiDocumentFinder.getById(wikiId);
        wikiAuthorizationService.requireDocumentLockPermission(document, actor);

        document.lock();
    }

    @Override
    public void unLock(Long wikiId, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);
        WikiDocument document = wikiDocumentFinder.getById(wikiId);
        wikiAuthorizationService.requireDocumentLockPermission(document, actor);

        document.unLock();
    }

    @Override
    public void delete(Long wikiId, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);
        WikiDocument document = wikiDocumentFinder.getById(wikiId);
        wikiAuthorizationService.requireDocumentDeletePermission(document, actor);

        document.softDelete();
    }

    @Override
    public void restore(Long wikiId, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getDeletedById(wikiId);
        wikiAuthorizationService.requireDocumentDeletePermission(document, actor);

        document.restoreSoftDeleted();
    }

    @Override
    public void hardDelete(Long wikiId, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);

        WikiDocument document = wikiDocumentFinder.getDeletedById(wikiId);
        wikiAuthorizationService.requireDocumentDeletePermission(document, actor);
        ensureNoChildren(wikiId);

        wikiDocumentCommandRepository.delete(document);
    }

    @Override
    public void batchHardDelete(Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);

        List<WikiDocument> deletedDocuments = wikiDocumentQueryRepository.findAllDeleted();
        for (WikiDocument document : deletedDocuments) {
            wikiAuthorizationService.requireDocumentDeletePermission(document, actor);
        }

        wikiDocumentTagRepository.deleteAllBySoftDeletedDocuments();
        wikiDocumentCommandRepository.deleteAllSoftDeleted();
    }

    private void ensureNoChildren(Long wikiId) {
        if (wikiDocumentQueryRepository.hasChildren(wikiId)) {
            throw new BadRequestException(DOCUMENT_HAS_CHILDREN);
        }
    }

    @Nullable
    private WikiDocument resolveParentDocument(@Nullable Long parentDocumentId) {
        if (parentDocumentId == null) {
            return null;
        }
        return wikiDocumentFinder.getById(parentDocumentId);
    }
}
