package com.tissue.feature.member.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberConstraintPolicy {
    public static final String USERNAME_REGEX = "^[a-z0-9]+$";
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 22;

    public static final String NAME_REGEX =
            "^[A-Za-z\\u00C0-\\u024F\\u0370-\\u03FF\\u0400-\\u04FF\\u4E00-\\u9FFF\\u3040-\\u30FF\\uAC00-\\uD7A3 ]+$";
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 35;

    public static final int DESCRIPTION_MAX_LENGTH = 255;
}
