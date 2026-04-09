package com.tissue.feature.issue.application.dto.response;

import java.io.InputStream;

public record FileDownloadResult(String originalFilename, String contentType, long fileSize, InputStream inputStream) {}
