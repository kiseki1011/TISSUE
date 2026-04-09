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
    LINK_TARGET_WORKSPACE_MISMATCH("Link target must belong to the same workspace as the source document");

    private final String defaultMessage;
}
