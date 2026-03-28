package com.tissue.feature.attachment.domain.exception;

import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_NOT_FOUND;
import static com.tissue.shared.exception.ErrorContextKeys.ATTACHMENT_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class AttachmentNotFoundException extends ResourceNotFoundException {

    public AttachmentNotFoundException(String issueKey, Long attachmentId) {
        super(ATTACHMENT_NOT_FOUND);
        addContext(ISSUE_KEY, issueKey);
        addContext(ATTACHMENT_ID, attachmentId);
    }
}
