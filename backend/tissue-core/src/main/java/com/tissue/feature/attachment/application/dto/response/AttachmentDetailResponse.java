package com.tissue.feature.attachment.application.dto.response;

import com.tissue.feature.attachment.domain.IssueAttachment;
import java.time.Instant;

public record AttachmentDetailResponse(
        Long attachmentId,
        String originalFilename,
        String contentType,
        long fileSize,
        Long uploadedBy,
        Instant createdAt) {

    public static AttachmentDetailResponse from(IssueAttachment attachment) {
        return new AttachmentDetailResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCreatedBy(),
                attachment.getCreatedAt());
    }
}
