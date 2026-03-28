package com.tissue.feature.attachment.application.service;

import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_STORAGE_FAILED;

import com.tissue.feature.attachment.application.dto.request.UploadAttachmentCommand;
import com.tissue.feature.attachment.application.dto.response.AttachmentUploadResponse;
import com.tissue.feature.attachment.application.port.repository.AttachmentRepository;
import com.tissue.feature.attachment.application.port.usecase.AttachmentCommandUseCase;
import com.tissue.feature.attachment.domain.IssueAttachment;
import com.tissue.feature.attachment.domain.exception.AttachmentNotFoundException;
import com.tissue.feature.attachment.domain.policy.IssueAttachmentPolicy;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.global.filestorage.FileStorageClient;
import com.tissue.global.filestorage.StoredFile;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.exception.base.InternalServerException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IssueAttachmentCommandService implements AttachmentCommandUseCase {

    private final AttachmentRepository attachmentRepository;
    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final AttachmentAuthorizationService attachmentAuthorizationService;
    private final FileStorageClient fileStorageClient;
    private final IssueAttachmentPolicy attachmentPolicy;

    @Override
    public AttachmentUploadResponse upload(IssueIdentifier iid, UploadAttachmentCommand cmd, Long memberId) {
        projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), memberId);
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());

        attachmentPolicy.ensureFileValid(cmd.fileSize(), cmd.contentType());
        long currentCount = attachmentRepository.countByIssueKeyAndWorkspaceKey(iid.issueKey(), iid.workspaceKey());
        attachmentPolicy.ensureAttachmentLimit(currentCount);

        String storedFilename = UUID.randomUUID() + extractExtension(cmd.originalFilename());
        String directory = iid.workspaceKey() + "/" + iid.issueKey();

        StoredFile storedFile;
        try {
            storedFile = fileStorageClient.store(directory, storedFilename, cmd.inputStream(), cmd.fileSize());
        } catch (Exception e) {
            throw new InternalServerException(ATTACHMENT_STORAGE_FAILED, e);
        }

        try {
            IssueAttachment attachment = IssueAttachment.create(
                    issue,
                    cmd.originalFilename(),
                    storedFilename,
                    cmd.contentType(),
                    cmd.fileSize(),
                    storedFile.storedPath());
            attachmentRepository.save(attachment);

            return new AttachmentUploadResponse(iid.issueKey(), attachment.getId(), cmd.originalFilename());
        } catch (Exception e) {
            fileStorageClient.delete(storedFile.storedPath());
            throw e;
        }
    }

    @Override
    public void delete(IssueIdentifier iid, Long attachmentId, Long memberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), memberId);
        IssueAttachment attachment = attachmentRepository
                .findWithIssueAndProjectByKeysAndId(iid.workspaceKey(), iid.issueKey(), attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(iid.issueKey(), attachmentId));

        attachmentAuthorizationService.requireDeletePermission(attachment, actor);

        fileStorageClient.delete(attachment.getStoredPath());
        attachmentRepository.delete(attachment);
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            return filename.substring(dotIndex);
        }
        return "";
    }
}
