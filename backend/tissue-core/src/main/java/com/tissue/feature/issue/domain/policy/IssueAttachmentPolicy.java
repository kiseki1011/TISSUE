package com.tissue.feature.issue.domain.policy;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_FILE_EMPTY;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_LIMIT_EXCEEDED;

import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IssueAttachmentPolicy {

    private final int maxAttachmentsPerIssue;
    private final List<String> allowedContentTypes;

    public void ensureFileNotEmpty(long fileSize) {
        if (fileSize <= 0) {
            throw new BadRequestException(ATTACHMENT_FILE_EMPTY);
        }
    }

    public void ensureContentTypeAllowed(String contentType) {
        if (!allowedContentTypes.contains(contentType)) {
            throw new BadRequestException(ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED);
        }
    }

    public void ensureAttachmentLimit(long currentCount) {
        if (currentCount >= maxAttachmentsPerIssue) {
            throw new ResourceConflictException(ATTACHMENT_LIMIT_EXCEEDED);
        }
    }
}
