package com.tissue.feature.issuetype.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IssueTypeConstraintPolicy {
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 50;

    public static final int DESCRIPTION_MAX_LENGTH = 255;
}
