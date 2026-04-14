package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.domain.IssueAttachment;
import java.time.Instant;

public record IssueAttachmentDetailResponse(
        Long attachmentId,
        String originalFilename,
        String contentType,
        long fileSize,
        Long uploadedBy,
        Instant createdAt) {

    public static IssueAttachmentDetailResponse from(IssueAttachment attachment) {
        return new IssueAttachmentDetailResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCreatedBy(),
                attachment.getCreatedAt());
    }
}
