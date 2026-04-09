package com.tissue.feature.wiki.application.service;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.ATTACHMENT_NOT_FOUND;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.ATTACHMENT_STORAGE_FAILED;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.LINK_NOT_FOUND;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.application.port.repository.WikiAttachmentRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentRepository;
import com.tissue.feature.wiki.application.port.repository.WikiLinkRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiAttachmentUseCase;
import com.tissue.feature.wiki.application.port.usecase.WikiUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiAttachment;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.WikiLink;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.feature.wiki.domain.policy.WikiAttachmentPolicy;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.global.file.FileContentDetector;
import com.tissue.global.filestorage.FileStorageClient;
import com.tissue.global.filestorage.StoredFile;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.InternalServerException;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WikiCommandService implements WikiUseCase, WikiAttachmentUseCase {

    private final WikiDocumentRepository wikiDocumentRepository;
    private final WikiSnapshotRepository wikiSnapshotRepository;
    private final WikiAttachmentRepository wikiAttachmentRepository;
    private final WikiLinkRepository wikiLinkRepository;
    private final WorkspaceFinder workspaceFinder;
    private final WikiDocumentFinder wikiDocumentFinder;
    private final WikiLinkTargetResolver wikiLinkTargetResolver;
    private final WikiAttachmentPolicy wikiAttachmentPolicy;
    private final FileStorageClient fileStorageClient;
    private final FileContentDetector fileContentDetector;

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
    public void uploadFile(String workspaceKey, Long wikiId, MultipartFile file, Long actorMemberId) {
        WikiDocument document = wikiDocumentFinder.getBy(workspaceKey, wikiId);

        wikiAttachmentPolicy.ensureFileValid(file.getSize(), file.getContentType());

        String detectedContentType = detectAndLogMismatch(file);

        wikiAttachmentPolicy.ensureContentTypeAllowed(detectedContentType);
        long currentCount = wikiAttachmentRepository.countByDocumentIdAndWorkspaceKey(wikiId, workspaceKey);
        wikiAttachmentPolicy.ensureAttachmentLimit(currentCount);

        String storedFilename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        String directory = workspaceKey + "/wiki/" + wikiId;

        StoredFile storedFile;
        try (var inputStream = file.getInputStream()) {
            storedFile = fileStorageClient.store(directory, storedFilename, inputStream, file.getSize());
        } catch (Exception e) {
            throw new InternalServerException(ATTACHMENT_STORAGE_FAILED, e);
        }

        try {
            WikiAttachment attachment = WikiAttachment.create(
                    document,
                    file.getOriginalFilename(),
                    storedFilename,
                    detectedContentType,
                    file.getSize(),
                    storedFile.storedPath());
            wikiAttachmentRepository.save(attachment);
        } catch (Exception e) {
            fileStorageClient.delete(storedFile.storedPath());
            throw e;
        }
    }

    @Override
    public void deleteAttachment(String workspaceKey, Long wikiId, Long attachmentId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiAttachment attachment = wikiAttachmentRepository
                .findByIdAndWorkspaceKey(attachmentId, workspaceKey)
                .orElseThrow(() -> new BadRequestException(ATTACHMENT_NOT_FOUND));

        fileStorageClient.delete(attachment.getStoredPath());
        wikiAttachmentRepository.delete(attachment);
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

    private String detectAndLogMismatch(MultipartFile file) {
        String detectedContentType;
        try (var inputStream = file.getInputStream()) {
            detectedContentType = fileContentDetector.detect(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            throw new InternalServerException(ATTACHMENT_STORAGE_FAILED, e);
        }

        if (!detectedContentType.equals(file.getContentType())) {
            log.warn(
                    "Content type mismatch - declared: {}, detected: {}, file: {}",
                    file.getContentType(),
                    detectedContentType,
                    file.getOriginalFilename());
        }

        return detectedContentType;
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            return filename.substring(dotIndex);
        }
        return "";
    }
}
