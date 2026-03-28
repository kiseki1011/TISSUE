package com.tissue.feature.attachment.application.dto.request;

import java.io.InputStream;

public record UploadAttachmentCommand(
        String originalFilename, String contentType, long fileSize, InputStream inputStream) {}
