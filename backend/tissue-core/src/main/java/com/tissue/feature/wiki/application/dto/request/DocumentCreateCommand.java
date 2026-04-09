package com.tissue.feature.wiki.application.dto.request;

import org.jspecify.annotations.Nullable;

public record DocumentCreateCommand(
        String title, String content, @Nullable Long parentDocumentId) {}
