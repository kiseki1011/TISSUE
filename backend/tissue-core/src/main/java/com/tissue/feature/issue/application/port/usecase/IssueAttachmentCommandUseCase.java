package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.response.IssueAttachmentUploadResponse;
import com.tissue.shared.dto.IssueIdentifier;
import org.springframework.web.multipart.MultipartFile;

public interface IssueAttachmentCommandUseCase {

    IssueAttachmentUploadResponse upload(IssueIdentifier iid, MultipartFile file, Long actorMemberId);

    void delete(IssueIdentifier iid, Long attachmentId, Long actorMemberId);
}
