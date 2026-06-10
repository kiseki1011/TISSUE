package com.tissue.feature.wiki.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WikiTagConstraintPolicy {

    public static final int MAX_TAGS_PER_DOCUMENT = 5;

    public static final int NAME_MIN_LENGTH = 1;
    public static final int NAME_MAX_LENGTH = 50;
}
