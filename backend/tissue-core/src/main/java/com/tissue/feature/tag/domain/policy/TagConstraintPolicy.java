package com.tissue.feature.tag.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagConstraintPolicy {
    public static final int NAME_MIN_LENGTH = 1;
    public static final int NAME_MAX_LENGTH = 50;

    public static final int DESCRIPTION_MAX_LENGTH = 255;
}
