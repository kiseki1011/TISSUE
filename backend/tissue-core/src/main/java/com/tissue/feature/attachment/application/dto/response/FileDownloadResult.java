package com.tissue.feature.attachment.application.dto.response;

import java.io.InputStream;

public record FileDownloadResult(String originalFilename, String contentType, long fileSize, InputStream inputStream) {}
