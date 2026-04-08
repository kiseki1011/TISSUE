package com.tissue.feature.attachment.application.port.usecase;

import com.tissue.feature.attachment.application.dto.response.AttachmentUploadResponse;
import com.tissue.shared.dto.IssueIdentifier;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentCommandUseCase {

    AttachmentUploadResponse upload(IssueIdentifier iid, MultipartFile file, Long memberId);

    void delete(IssueIdentifier iid, Long attachmentId, Long memberId);
}
