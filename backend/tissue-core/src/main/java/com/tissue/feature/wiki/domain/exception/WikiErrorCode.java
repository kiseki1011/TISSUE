package com.tissue.feature.wiki.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WikiErrorCode implements ErrorCode {
    DOCUMENT_NOT_FOUND("Wiki document not found"),
    DOCUMENT_LOCKED("Cannot edit a locked document"),
    PARENT_WORKSPACE_MISMATCH("Parent document must belong to the same workspace as the child document"),
    LINK_NOT_FOUND("Wiki link not found"),
    LINK_TARGET_NOT_FOUND("Wiki link target not found"),
    LINK_TARGET_WORKSPACE_MISMATCH("Link target must belong to the same workspace as the source document"),
    ATTACHMENT_NOT_FOUND("Wiki attachment not found"),
    ATTACHMENT_FILE_EMPTY("Uploaded file is empty"),
    ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED("Content type is not allowed"),
    ATTACHMENT_LIMIT_EXCEEDED("Maximum number of attachments per document exceeded"),
    ATTACHMENT_STORAGE_FAILED("Failed to store wiki attachment");

    private final String defaultMessage;
}
