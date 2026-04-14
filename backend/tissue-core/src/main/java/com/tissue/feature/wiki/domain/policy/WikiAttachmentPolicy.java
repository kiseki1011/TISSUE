package com.tissue.feature.wiki.domain.policy;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.ATTACHMENT_FILE_EMPTY;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.ATTACHMENT_LIMIT_EXCEEDED;

import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WikiAttachmentPolicy {

    private final int maxAttachmentsPerDocument;
    private final List<String> allowedContentTypes;

    public void ensureFileValid(long fileSize, String contentType) {
        if (fileSize <= 0) {
            throw new BadRequestException(ATTACHMENT_FILE_EMPTY);
        }
        ensureContentTypeAllowed(contentType);
    }

    public void ensureContentTypeAllowed(String contentType) {
        if (!allowedContentTypes.contains(contentType)) {
            throw new BadRequestException(ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED);
        }
    }

    public void ensureAttachmentLimit(long currentCount) {
        if (currentCount >= maxAttachmentsPerDocument) {
            throw new BadRequestException(ATTACHMENT_LIMIT_EXCEEDED);
        }
    }
}
