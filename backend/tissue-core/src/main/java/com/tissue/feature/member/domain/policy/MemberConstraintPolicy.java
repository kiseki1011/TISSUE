package com.tissue.feature.member.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberConstraintPolicy {
    public static final String USERNAME_REGEX = "^[a-z0-9]+$";
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 22;

    public static final String NAME_REGEX = "^[\\p{L}]+$";
    public static final int NAME_MIN_LENGTH = 3;
    public static final int NAME_MAX_LENGTH = 35;
}
