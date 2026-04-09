package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiAttachment;
import java.time.Instant;

public record WikiAttachmentDetailResponse(
        Long attachmentId,
        String originalFilename,
        String contentType,
        long fileSize,
        Long uploadedBy,
        Instant createdAt) {

    public static WikiAttachmentDetailResponse from(WikiAttachment attachment) {
        return new WikiAttachmentDetailResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCreatedBy(),
                attachment.getCreatedAt());
    }
}
