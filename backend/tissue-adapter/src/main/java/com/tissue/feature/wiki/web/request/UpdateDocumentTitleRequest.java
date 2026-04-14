package com.tissue.feature.wiki.web.request;

import static com.tissue.feature.wiki.domain.policy.WikiDocumentConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.wiki.domain.policy.WikiDocumentConstraintPolicy.TITLE_MIN_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDocumentTitleRequest(
        @NotBlank @Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH)
        String title) {}
