package com.tissue.feature.attachment.application.service;

import com.tissue.feature.attachment.application.dto.response.AttachmentDetailResponse;
import com.tissue.feature.attachment.application.dto.response.FileDownloadResult;
import com.tissue.feature.attachment.application.port.repository.AttachmentQueryRepository;
import com.tissue.feature.attachment.application.port.repository.AttachmentRepository;
import com.tissue.feature.attachment.application.port.usecase.AttachmentQueryUseCase;
import com.tissue.feature.attachment.domain.IssueAttachment;
import com.tissue.feature.attachment.domain.exception.AttachmentNotFoundException;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.global.filestorage.FileResource;
import com.tissue.global.filestorage.FileStorageClient;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueAttachmentQueryService implements AttachmentQueryUseCase {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentQueryRepository attachmentQueryRepository;
    private final ProjectMemberFinder projectMemberFinder;
    private final FileStorageClient fileStorageClient;

    @Override
    public List<AttachmentDetailResponse> getIssueAttachments(IssueIdentifier iid, Long memberId) {
        projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), memberId);

        return attachmentQueryRepository.findByIssue(iid.workspaceKey(), iid.issueKey()).stream()
                .map(AttachmentDetailResponse::from)
                .toList();
    }

    @Override
    public FileDownloadResult download(IssueIdentifier iid, Long attachmentId, Long memberId) {
        projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), memberId);

        IssueAttachment attachment = attachmentRepository
                .findWithIssueAndProjectByKeysAndId(iid.workspaceKey(), iid.issueKey(), attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(iid.issueKey(), attachmentId));

        FileResource resource = fileStorageClient
                .load(attachment.getStoredPath())
                .orElseThrow(() -> new AttachmentNotFoundException(iid.issueKey(), attachmentId));

        return new FileDownloadResult(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                resource.fileSize(),
                resource.inputStream());
    }
}
