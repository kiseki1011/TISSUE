package com.tissue.feature.wiki.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WikiDocumentConstraintPolicy {

    public static final int TITLE_MIN_LENGTH = 1;
    public static final int TITLE_MAX_LENGTH = 200;

    public static final int CONTENT_MAX_LENGTH = 100000;

    public static final int EDIT_REASON_MAX_LENGTH = 255;

    public static final int SEARCH_KEYWORD_MIN_LENGTH = 1;
    public static final int SEARCH_KEYWORD_MAX_LENGTH = 200;

    public static final int SEARCH_DEFAULT_LIMIT = 20;
    public static final int SEARCH_MAX_LIMIT = 100;
}
