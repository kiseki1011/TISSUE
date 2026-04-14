package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ATTACHMENT_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class IssueAttachmentNotFoundException extends ResourceNotFoundException {

    public IssueAttachmentNotFoundException(String issueKey, Long attachmentId) {
        super(IssueErrorCode.ATTACHMENT_NOT_FOUND);
        addContext(ISSUE_KEY, issueKey);
        addContext(ATTACHMENT_ID, attachmentId);
    }
}
