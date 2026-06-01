package com.tissue.global.file;

import com.tissue.feature.issue.config.IssueAttachmentProperties;
import com.tissue.feature.wiki.config.WikiAttachmentProperties;
import java.util.List;

/**
 * Shared default allow-list of attachment content types, reused as the default value by every
 * attachment properties. ({@link IssueAttachmentProperties}, {@link WikiAttachmentProperties})
 */
public final class AttachmentContentTypes {

    public static final List<String> DEFAULT = List.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "image/heic",
            "image/heif",
            "image/avif",
            "image/svg+xml",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/x-hwp",
            "application/hwp+zip",
            "text/plain",
            "text/csv",
            "text/markdown",
            "text/x-web-markdown",
            "application/json",
            "application/xml",
            "application/zip",
            "application/gzip");

    private AttachmentContentTypes() {}
}
