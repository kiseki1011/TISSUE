package com.tissue.feature.attachment.application.port.usecase;

import com.tissue.feature.attachment.application.dto.request.UploadAttachmentCommand;
import com.tissue.feature.attachment.application.dto.response.AttachmentUploadResponse;
import com.tissue.shared.dto.IssueIdentifier;

public interface AttachmentCommandUseCase {

    AttachmentUploadResponse upload(IssueIdentifier iid, UploadAttachmentCommand command, Long memberId);

    void delete(IssueIdentifier iid, Long attachmentId, Long memberId);
}
