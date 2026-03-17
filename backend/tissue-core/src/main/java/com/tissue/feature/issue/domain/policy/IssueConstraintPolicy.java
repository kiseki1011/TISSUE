package com.tissue.feature.issue.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IssueConstraintPolicy {
    public static final int TITLE_MIN_LENGTH = 2;
    public static final int TITLE_MAX_LENGTH = 100;

    public static final int CONTENT_MAX_LENGTH = 65535;

    public static final int SUMMARY_MAX_LENGTH = 2000;
}
