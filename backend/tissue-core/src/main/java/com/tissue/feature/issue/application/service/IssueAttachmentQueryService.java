package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.response.FileDownloadResult;
import com.tissue.feature.issue.application.dto.response.IssueAttachmentDetailResponse;
import com.tissue.feature.issue.application.port.repository.IssueAttachmentRepository;
import com.tissue.feature.issue.application.port.usecase.IssueAttachmentQueryUseCase;
import com.tissue.feature.issue.domain.IssueAttachment;
import com.tissue.feature.issue.domain.exception.IssueAttachmentNotFoundException;
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
public class IssueAttachmentQueryService implements IssueAttachmentQueryUseCase {

    private final IssueAttachmentRepository issueAttachmentRepository;
    private final ProjectMemberFinder projectMemberFinder;
    private final FileStorageClient fileStorageClient;

    @Override
    public List<IssueAttachmentDetailResponse> getIssueAttachments(IssueIdentifier iid, Long actorMemberId) {
        projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        return issueAttachmentRepository.findByIssueKey(iid.issueKey()).stream()
                .map(IssueAttachmentDetailResponse::from)
                .toList();
    }

    @Override
    public FileDownloadResult download(IssueIdentifier iid, Long attachmentId, Long actorMemberId) {
        projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        IssueAttachment attachment = issueAttachmentRepository
                .findWithIssueAndProjectByIssueKeyAndId(iid.issueKey(), attachmentId)
                .orElseThrow(() -> new IssueAttachmentNotFoundException(iid.issueKey(), attachmentId));

        FileResource resource = fileStorageClient
                .load(attachment.getStoredPath())
                .orElseThrow(() -> new IssueAttachmentNotFoundException(iid.issueKey(), attachmentId));

        return new FileDownloadResult(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                resource.fileSize(),
                resource.inputStream());
    }
}
