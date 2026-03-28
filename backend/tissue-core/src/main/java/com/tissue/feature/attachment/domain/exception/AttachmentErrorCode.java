package com.tissue.feature.attachment.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttachmentErrorCode implements ErrorCode {
    ATTACHMENT_NOT_FOUND("Attachment not found"),
    ATTACHMENT_DELETE_NOT_ALLOWED("Must be the uploader or admin to delete the attachment"),
    ATTACHMENT_FILE_EMPTY("Uploaded file is empty"),
    ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED("Content type is not allowed"),
    ATTACHMENT_FILE_SIZE_EXCEEDED("File size exceeds the maximum allowed size"),
    ATTACHMENT_LIMIT_EXCEEDED("Maximum number of attachments per issue exceeded"),
    ATTACHMENT_STORAGE_FAILED("Failed to store the attachment file");

    private final String defaultMessage;
}
