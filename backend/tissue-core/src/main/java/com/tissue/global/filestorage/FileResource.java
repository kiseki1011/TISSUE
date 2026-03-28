package com.tissue.global.filestorage;

import java.io.InputStream;

public record FileResource(InputStream inputStream, long fileSize) {}
