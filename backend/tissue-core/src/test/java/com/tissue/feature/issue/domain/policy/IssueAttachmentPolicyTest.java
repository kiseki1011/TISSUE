package com.tissue.feature.issue.domain.policy;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_FILE_EMPTY;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_LIMIT_EXCEEDED;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueAttachmentPolicyTest {

    private final int maxAttachmentsPerIssue = 5;
    private final List<String> allowedContentTypes = List.of("image/png", "image/jpeg", "application/pdf");

    private final IssueAttachmentPolicy policy = new IssueAttachmentPolicy(maxAttachmentsPerIssue, allowedContentTypes);

    @Nested
    @DisplayName("ensure file is not empty")
    class EnsureFileNotEmpty {

        @Test
        @DisplayName("success: if file size is positive, it is valid")
        void successNonEmptyFile() {
            assertThatNoException().isThrownBy(() -> policy.ensureFileNotEmpty(1024));
        }

        @Test
        @DisplayName("fail: if empty, throws BadRequestException")
        void failEmptyFile() {
            assertThatThrownBy(() -> policy.ensureFileNotEmpty(0))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_FILE_EMPTY);
        }
    }

    @Nested
    @DisplayName("ensure content type is allowed")
    class EnsureContentTypeAllowed {

        @Test
        @DisplayName("success: if content type is in the allow-list, it is valid")
        void successAllowedContentType() {
            assertThatNoException().isThrownBy(() -> policy.ensureContentTypeAllowed("image/png"));
        }

        @Test
        @DisplayName("fail: if not allowed content type, throws BadRequestException")
        void failContentTypeNotAllowed() {
            assertThatThrownBy(() -> policy.ensureContentTypeAllowed("application/exe"))
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
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_LIMIT_EXCEEDED);
        }
    }
}
