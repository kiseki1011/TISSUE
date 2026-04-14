package com.tissue.feature.wiki.web.request;

import org.jspecify.annotations.Nullable;

public record SetDocumentParentRequest(@Nullable Long parentDocumentId) {}
