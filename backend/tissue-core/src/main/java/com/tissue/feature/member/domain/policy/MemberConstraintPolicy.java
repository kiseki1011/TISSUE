package com.tissue.feature.member.domain.policy;

public interface MemberConstraintPolicy {
    String USERNAME_REGEX = "^[a-z0-9]+$";
    int USERNAME_MIN_LENGTH = 3;
    int USERNAME_MAX_LENGTH = 22;

    String NAME_REGEX = "^[\\p{L}]+$";
    int NAME_MIN_LENGTH = 3;
    int NAME_MAX_LENGTH = 35;
}
