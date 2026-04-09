package com.tissue.feature.wiki.application.service;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.ATTACHMENT_STORAGE_FAILED;

import com.tissue.feature.wiki.application.dto.response.FileDownloadResult;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentDetailResponse;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentUploadResponse;
import com.tissue.feature.wiki.application.port.repository.WikiAttachmentRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiAttachmentUseCase;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiAttachment;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.exception.WikiAttachmentNotFoundException;
import com.tissue.feature.wiki.domain.policy.WikiAttachmentPolicy;
import com.tissue.global.file.FileContentDetector;
import com.tissue.global.filestorage.FileResource;
import com.tissue.global.filestorage.FileStorageClient;
import com.tissue.global.filestorage.StoredFile;
import com.tissue.shared.exception.base.InternalServerException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiAttachmentService implements WikiAttachmentUseCase {

    private final WikiAttachmentRepository wikiAttachmentRepository;
    private final WikiDocumentFinder wikiDocumentFinder;
    private final WikiAttachmentPolicy wikiAttachmentPolicy;
    private final FileStorageClient fileStorageClient;
    private final FileContentDetector fileContentDetector;

    @Override
    @Transactional
    public WikiAttachmentUploadResponse uploadFile(
            String workspaceKey, Long wikiId, MultipartFile file, Long actorMemberId) {
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

            return new WikiAttachmentUploadResponse(attachment.getId(), attachment.getOriginalFilename());
        } catch (Exception e) {
            fileStorageClient.delete(storedFile.storedPath());
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteAttachment(String workspaceKey, Long wikiId, Long attachmentId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiAttachment attachment = wikiAttachmentRepository
                .findByIdAndWorkspaceKey(attachmentId, workspaceKey)
                .orElseThrow(() -> new WikiAttachmentNotFoundException(wikiId, attachmentId));

        fileStorageClient.delete(attachment.getStoredPath());
        wikiAttachmentRepository.delete(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WikiAttachmentDetailResponse> getWikiAttachments(String workspaceKey, Long wikiId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        return wikiAttachmentRepository.findByDocumentIdAndWorkspaceKey(wikiId, workspaceKey).stream()
                .map(WikiAttachmentDetailResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadResult download(String workspaceKey, Long wikiId, Long attachmentId, Long actorMemberId) {
        wikiDocumentFinder.getBy(workspaceKey, wikiId);

        WikiAttachment attachment = wikiAttachmentRepository
                .findByIdAndWorkspaceKey(attachmentId, workspaceKey)
                .orElseThrow(() -> new WikiAttachmentNotFoundException(wikiId, attachmentId));

        FileResource resource = fileStorageClient
                .load(attachment.getStoredPath())
                .orElseThrow(() -> new WikiAttachmentNotFoundException(wikiId, attachmentId));

        return new FileDownloadResult(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                resource.fileSize(),
                resource.inputStream());
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
