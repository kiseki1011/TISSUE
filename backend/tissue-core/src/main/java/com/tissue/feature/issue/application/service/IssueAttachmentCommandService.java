package com.tissue.feature.issue.application.service;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_STORAGE_FAILED;

import com.tissue.feature.issue.application.dto.response.IssueAttachmentUploadResponse;
import com.tissue.feature.issue.application.port.repository.IssueAttachmentRepository;
import com.tissue.feature.issue.application.port.usecase.IssueAttachmentCommandUseCase;
import com.tissue.feature.issue.application.service.authorization.IssueAttachmentAuthorizationService;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueAttachment;
import com.tissue.feature.issue.domain.exception.IssueAttachmentNotFoundException;
import com.tissue.feature.issue.domain.policy.IssueAttachmentPolicy;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.global.file.FileContentDetector;
import com.tissue.global.filestorage.FileStorageClient;
import com.tissue.global.filestorage.StoredFile;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.exception.base.InternalServerException;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IssueAttachmentCommandService implements IssueAttachmentCommandUseCase {

    private final IssueAttachmentRepository issueAttachmentRepository;
    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueAttachmentAuthorizationService issueAttachmentAuthorizationService;
    private final FileStorageClient fileStorageClient;
    private final IssueAttachmentPolicy attachmentPolicy;
    private final FileContentDetector fileContentDetector;

    @Override
    public IssueAttachmentUploadResponse upload(IssueIdentifier iid, MultipartFile file, Long actorMemberId) {
        projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);
        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        attachmentPolicy.ensureFileValid(file.getSize(), file.getContentType());

        String detectedContentType = detectAndLogMismatch(file);

        attachmentPolicy.ensureContentTypeAllowed(detectedContentType);
        long currentCount = issueAttachmentRepository.countByIssueKey(iid.issueKey());
        attachmentPolicy.ensureAttachmentLimit(currentCount);

        String storedFilename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        String directory = iid.projectKey() + "/" + iid.issueKey();

        StoredFile storedFile;
        try (var inputStream = file.getInputStream()) {
            storedFile = fileStorageClient.store(directory, storedFilename, inputStream, file.getSize());
        } catch (Exception e) {
            throw new InternalServerException(ATTACHMENT_STORAGE_FAILED, e);
        }

        try {
            IssueAttachment attachment = IssueAttachment.create(
                    issue,
                    file.getOriginalFilename(),
                    storedFilename,
                    detectedContentType,
                    file.getSize(),
                    storedFile.storedPath());
            issueAttachmentRepository.save(attachment);

            return new IssueAttachmentUploadResponse(attachment.getId(), file.getOriginalFilename());

        } catch (Exception e) {
            fileStorageClient.delete(storedFile.storedPath());
            throw e;
        }
    }

    @Override
    public void delete(IssueIdentifier iid, Long attachmentId, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);
        IssueAttachment attachment = issueAttachmentRepository
                .findWithIssueAndProjectByIssueKeyAndId(iid.issueKey(), attachmentId)
                .orElseThrow(() -> new IssueAttachmentNotFoundException(iid.issueKey(), attachmentId));

        issueAttachmentAuthorizationService.requireDeletePermission(attachment, actor);

        fileStorageClient.delete(attachment.getStoredPath());
        issueAttachmentRepository.delete(attachment);
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
