package com.tissue.feature.wiki.application.dto.response;

import java.io.InputStream;

public record FileDownloadResult(String originalFilename, String contentType, long fileSize, InputStream inputStream) {}
