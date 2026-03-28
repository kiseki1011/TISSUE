package com.tissue.feature.attachment.domain.policy;

import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED;
import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_FILE_EMPTY;
import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_FILE_SIZE_EXCEEDED;
import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_LIMIT_EXCEEDED;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueAttachmentPolicyTest {

    private final long maxFileSize = 10 * 1024 * 1024;
    private final int maxAttachmentsPerIssue = 5;
    private final List<String> allowedContentTypes = List.of("image/png", "image/jpeg", "application/pdf");

    private final IssueAttachmentPolicy policy =
            new IssueAttachmentPolicy(maxFileSize, maxAttachmentsPerIssue, allowedContentTypes);

    @Nested
    @DisplayName("ensure file is valid")
    class EnsureFileValid {

        @Test
        @DisplayName("success: if file is under max file size and is an allowed content type, it is valid")
        void successValidFile() {
            assertThatNoException().isThrownBy(() -> policy.ensureFileValid(1024, "image/png"));
        }

        @Test
        @DisplayName("fail: if empty, throws BadRequestException")
        void failEmptyFile() {
            assertThatThrownBy(() -> policy.ensureFileValid(0, "image/png"))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_FILE_EMPTY);
        }

        @Test
        @DisplayName("fail: if file size exceeded, throws BadRequestException")
        void failFileSizeExceeded() {
            assertThatThrownBy(() -> policy.ensureFileValid(20 * 1024 * 1024, "image/png"))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_FILE_SIZE_EXCEEDED);
        }

        @Test
        @DisplayName("fail: if not allowed content type, throws BadRequestException")
        void failContentTypeNotAllowed() {
            assertThatThrownBy(() -> policy.ensureFileValid(1024, "application/exe"))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("ensure under issue attachment limit")
    class EnsureAttachmentLimit {

        @Test
        @DisplayName("success: under limit")
        void successUnderLimit() {
            assertThatNoException().isThrownBy(() -> policy.ensureAttachmentLimit(4));
        }

        @Test
        @DisplayName("fail: if limit exceeded, throws BadRequestException")
        void failLimitExceeded() {
            assertThatThrownBy(() -> policy.ensureAttachmentLimit(5))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_LIMIT_EXCEEDED);
        }
    }
}
